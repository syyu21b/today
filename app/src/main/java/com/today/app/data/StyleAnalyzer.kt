package com.today.app.data

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PointF
import android.graphics.RectF
import androidx.core.graphics.ColorUtils
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.Closeable
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** Body part a region was sampled from (labels are localized in the UI). */
enum class BodyPart { HAIR, SKIN, TOP, BOTTOM, SHOES, OUTFIT }

/** What the analyzer concluded (rendered as a localized summary). */
enum class StyleNote { BACK_VIEW, FACE_HIDDEN, LONG_HAIR, STRIPED, SHORT_SLEEVE, LONG_SLEEVE, SHORTS, LONG_PANTS, UPPER_BODY_ONLY }

/** A part of the photo the analyzer looked at (for the overlay on the preview). */
data class StyleRegion(val part: BodyPart, val rect: RectF, val color: Int)

data class StyleResult(
    val outfit: Outfit,
    val regions: List<StyleRegion>,
    val personDetected: Boolean,
    val notes: List<StyleNote>,
)

/**
 * Turns a photo into a pixel-avatar outfit:
 *  1. ML Kit pose detection finds body landmarks (eyes, shoulders, elbows, hips, knees, ankles…).
 *  2. ML Kit selfie segmentation masks out the background.
 *  3. For each body region (hair, face, top, bottom, shoes) we k-means the person pixels
 *     and take the dominant color; extra checks detect stripes, short sleeves, shorts and long hair.
 * Everything runs on-device with bundled models (no network).
 */
class StyleAnalyzer : Closeable {

    private val poseDetector by lazy {
        PoseDetection.getClient(
            AccuratePoseDetectorOptions.Builder()
                .setDetectorMode(AccuratePoseDetectorOptions.SINGLE_IMAGE_MODE)
                .build()
        )
    }
    private val segmenter by lazy {
        Segmentation.getClient(
            SelfieSegmenterOptions.Builder()
                .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
                .build()
        )
    }
    private var poseUsed = false
    private var segUsed = false

    suspend fun analyze(bitmap: Bitmap, base: Outfit, name: String): StyleResult {
        val image = InputImage.fromBitmap(bitmap, 0)
        poseUsed = true
        val pose = runCatching { poseDetector.process(image).await() }.getOrNull()
        segUsed = true
        val mask = runCatching {
            val m = segmenter.process(image).await()
            val buffer = m.buffer
            buffer.rewind()
            val values = FloatArray(m.width * m.height)
            buffer.asFloatBuffer().get(values)
            Mask(values, m.width, m.height)
        }.getOrNull()

        return withContext(Dispatchers.Default) {
            val img = Img(bitmap, mask)
            analyzeWithPose(img, pose, base, name) ?: analyzeWithoutPerson(img, base, name)
        }
    }

    override fun close() {
        if (poseUsed) poseDetector.close()
        if (segUsed) segmenter.close()
    }

    // ------------------------------------------------------------------ person path

    private fun analyzeWithPose(img: Img, pose: Pose?, base: Outfit, name: String): StyleResult? {
        if (pose == null) return null
        fun lm(type: Int): PointF? = pose.getPoseLandmark(type)
            ?.takeIf { it.inFrameLikelihood >= 0.5f }
            ?.position
            ?.takeIf { it.x >= 0 && it.y >= 0 && it.x < img.w && it.y < img.h }

        val ls = lm(PoseLandmark.LEFT_SHOULDER) ?: return null
        val rs = lm(PoseLandmark.RIGHT_SHOULDER) ?: return null
        val shoulderW = max(dist(ls, rs), img.minDim * 0.08f)
        val shoulderY = (ls.y + rs.y) / 2
        val sxMin = min(ls.x, rs.x)
        val sxMax = max(ls.x, rs.x)

        val regions = mutableListOf<StyleRegion>()
        val notes = mutableListOf<StyleNote>()

        // Facing away from the camera, the person's LEFT shoulder shows on the image's left.
        val backView = ls.x < rs.x

        val lh = lm(PoseLandmark.LEFT_HIP)
        val rh = lm(PoseLandmark.RIGHT_HIP)
        val hipY = if (lh != null && rh != null) (lh.y + rh.y) / 2 else null
        val torsoH = hipY?.let { it - shoulderY }?.takeIf { it > shoulderW * 0.3f }
        val lElbow = lm(PoseLandmark.LEFT_ELBOW)
        val rElbow = lm(PoseLandmark.RIGHT_ELBOW)
        val lWrist = lm(PoseLandmark.LEFT_WRIST)
        val rWrist = lm(PoseLandmark.RIGHT_WRIST)
        val lKnee = lm(PoseLandmark.LEFT_KNEE)
        val rKnee = lm(PoseLandmark.RIGHT_KNEE)
        val lAnkle = lm(PoseLandmark.LEFT_ANKLE)
        val rAnkle = lm(PoseLandmark.RIGHT_ANKLE)

        // ---- face / skin & hair ----
        val le = lm(PoseLandmark.LEFT_EYE)
        val re = lm(PoseLandmark.RIGHT_EYE)
        var skin = base.skin
        var skinKnown = false
        var faceSeen = false
        var hair = base.hair
        var longHair = base.longHair
        // All tones seen on the head (sunlit + shaded hair) — used to spot hair over the back.
        var hairTones = emptyList<Int>()
        if (le != null && re != null) {
            val e = max(dist(le, re), shoulderW * 0.22f)
            val eyeX = (le.x + re.x) / 2
            val eyeY = (le.y + re.y) / 2

            val faceRect = RectF(eyeX - e * 0.9f, eyeY + e * 0.3f, eyeX + e * 0.9f, eyeY + e * 0.75f)
            val hairRect = RectF(eyeX - e * 1.1f, eyeY - e * 1.9f, eyeX + e * 1.1f, eyeY - e * 0.7f)
            val faceColor = img.clusters(faceRect).firstOrNull()?.color
            // Strict mask: never let sky / trees above the head count as hair.
            val hairClusters = img.clusters(hairRect, fallbackToAll = false)
            val topOfHead = hairClusters.firstOrNull()?.color

            // Pose estimates eyes even from behind, so only trust the "face" when the person faces
            // the camera and it looks like skin.
            if (!backView && faceColor != null && isSkinTone(faceColor)) {
                skin = faceColor
                skinKnown = true
                faceSeen = true
                regions += StyleRegion(BodyPart.SKIN, faceRect, skin)
            }

            if (faceSeen) {
                // Forehead leaks into the box (more so with a tilted head): take the biggest
                // cluster that is NOT this person's skin; if it's all skin, they're bald/shaved.
                hair = hairClusters.firstOrNull { it.share > 0.1f && colorDist(it.color, skin) > 70 }?.color ?: topOfHead ?: hair
                if (hairClusters.isNotEmpty()) regions += StyleRegion(BodyPart.HAIR, hairRect, hair)

                // Long hair: hair-colored person pixels beside the neck, between chin and shoulders.
                val sideTop = eyeY + e * 1.5f
                val sideBottom = shoulderY - (shoulderY - eyeY) * 0.08f
                if (sideBottom - sideTop > e * 0.3f) {
                    longHair = listOf(eyeX - e * 1.25f, eyeX + e * 1.25f).any { cx ->
                        img.fraction(RectF(cx - e * 0.35f, sideTop, cx + e * 0.35f, sideBottom), requirePerson = true) {
                            colorDist(it, hair) < 70 && colorDist(it, skin) > 50
                        } > 0.4f
                    }
                }
            } else {
                // Back of the head (or a hidden face): the middle of the head is hair.
                val headRect = RectF(eyeX - e * 0.8f, eyeY - e * 0.9f, eyeX + e * 0.8f, eyeY + e * 0.9f)
                val headClusters = img.clusters(headRect, fallbackToAll = false)
                val headPick = if (backView) headClusters.firstOrNull()
                else headClusters.firstOrNull { it.share > 0.2f && !isSkinTone(it.color) } ?: headClusters.firstOrNull()
                headPick?.let {
                    hair = it.color
                    regions += StyleRegion(BodyPart.HAIR, headRect, hair)
                }
                hairTones = headClusters.filter { it.share > 0.15f }.map { it.color }.ifEmpty { listOf(hair) }
                notes += if (backView) StyleNote.BACK_VIEW else StyleNote.FACE_HIDDEN
                // Long hair from behind: hair covers the upper back, so the upper back either matches
                // a head tone or looks different from the lower back (a plain top is one color).
                val span = torsoH ?: shoulderW
                val upperBack = RectF(sxMin + shoulderW * 0.15f, shoulderY, sxMax - shoulderW * 0.15f, shoulderY + span * 0.45f)
                val lowerBack = RectF(sxMin + shoulderW * 0.15f, shoulderY + span * 0.65f, sxMax - shoulderW * 0.15f, shoulderY + span * 0.95f)
                val upperColor = img.clusters(upperBack).firstOrNull()?.color
                val lowerColor = if (torsoH != null) img.clusters(lowerBack).firstOrNull()?.color else null
                val matchesHead = img.fraction(upperBack, requirePerson = true) { c -> hairTones.any { colorDist(c, it) < 60 } } > 0.35f
                val differsFromLower = upperColor != null && lowerColor != null && colorDist(upperColor, lowerColor) > 70
                longHair = matchesHead || differsFromLower
                if (differsFromLower) hairTones = hairTones + upperColor
            }
            if (longHair) notes += StyleNote.LONG_HAIR
        }

        // No usable face: take skin from bare forearms / shins instead.
        if (!skinKnown) {
            val limbSamples = listOf(lElbow to lWrist, rElbow to rWrist, lKnee to lAnkle, rKnee to rAnkle)
                .flatMap { (a, b) ->
                    if (a == null || b == null) {
                        emptyList()
                    } else {
                        (0 until 8).map { i ->
                            val t = 0.2f + 0.6f * i / 7
                            img.patch(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
                        }
                    }
                }
                .filter(::isSkinTone)
            if (limbSamples.size >= 4) {
                kmeans(limbSamples.toIntArray(), 2).firstOrNull()?.let {
                    skin = it.color
                    skinKnown = true
                }
            }
        }

        // Must look like skin in general AND (when known) like this person's skin, so white or
        // beige clothes next to pale skin aren't mistaken for bare skin.
        fun isSkin(c: Int) = isSkinTone(c) && (!skinKnown || colorDist(c, skin) < 75)

        // ---- top ----
        val topRect = if (hipY != null && torsoH != null) {
            RectF(sxMin + shoulderW * 0.2f, shoulderY + torsoH * 0.15f, sxMax - shoulderW * 0.2f, hipY - torsoH * 0.1f)
        } else {
            RectF(sxMin + shoulderW * 0.2f, shoulderY + shoulderW * 0.15f, sxMax - shoulderW * 0.2f, shoulderY + shoulderW * 0.9f)
        }
        val hairOnBack = longHair && !faceSeen
        val topClusters = img.clusters(topRect, k = 4)
            // From behind, long hair covers the back: don't mistake it for the shirt.
            .filterNot { cl -> hairOnBack && hairTones.any { colorDist(cl.color, it) < 60 } && cl.share < 0.85f }
            .let { list -> list.filterNot { isSkin(it.color) && it.share < 0.6f }.ifEmpty { list } }
        val shirt = topClusters.firstOrNull()?.color ?: base.shirt
        regions += StyleRegion(BodyPart.TOP, topRect, shirt)

        // Stripes: two strong, distinct colors that alternate down the torso.
        var striped = false
        var stripe = base.stripe
        val second = topClusters.getOrNull(1)
        if (second != null && second.share > 0.2f && colorDist(shirt, second.color) > 100 &&
            img.alternations(topRect, shirt, second.color) >= 4
        ) {
            striped = true
            stripe = second.color
            notes += StyleNote.STRIPED
        }

        val accent = topClusters.drop(if (striped) 2 else 1)
            .firstOrNull { it.share > 0.08f && colorDist(it.color, shirt) > 90 && !isSkin(it.color) }
            ?.color
            ?: contrastOf(shirt)

        // ---- sleeves: does the forearm look like the top (sleeve) or not (bare arm)? ----
        // Comparing against the shirt as well as skin keeps tattoos / gloves / shadows from
        // being mistaken for long sleeves.
        fun isSleeve(c: Int) = colorDist(c, shirt) < 85 || (striped && colorDist(c, stripe) < 85)
        fun looksBare(a: PointF, b: PointF, t0: Float, t1: Float): Boolean {
            val skinFrac = img.segmentFraction(a, b, t0, t1, ::isSkin)
            val sleeveFrac = img.segmentFraction(a, b, t0, t1) { isSleeve(it) && !isSkin(it) }
            return skinFrac >= 0.5f || sleeveFrac < 0.3f
        }
        var shortSleeve = base.shortSleeve
        val armVotes = listOf(
            Triple(ls, lElbow, lWrist),
            Triple(rs, rElbow, rWrist),
        ).mapNotNull { (shoulder, elbow, wrist) ->
            when {
                elbow != null && wrist != null -> looksBare(elbow, wrist, 0.25f, 0.8f)
                elbow != null -> looksBare(shoulder, elbow, 0.5f, 0.9f) // only upper arm visible
                else -> null
            }
        }
        if (armVotes.isNotEmpty()) shortSleeve = armVotes.count { it } * 2 >= armVotes.size
        notes += if (shortSleeve) StyleNote.SHORT_SLEEVE else StyleNote.LONG_SLEEVE

        // ---- bottom ----
        var pants = base.pants
        var shorts = base.shorts
        if (lh != null && rh != null && hipY != null) {
            val hipW = max(dist(lh, rh), shoulderW * 0.5f)
            val hxMin = min(lh.x, rh.x)
            val hxMax = max(lh.x, rh.x)
            val lk = lKnee
            val rk = rKnee
            val la = lAnkle
            val ra = rAnkle

            // Thigh color = what the bottoms look like; bare shins differ from it.
            val thigh = listOfNotNull(
                lk?.let { img.patch((lh.x + it.x) / 2, (lh.y + it.y) / 2, 3) },
                rk?.let { img.patch((rh.x + it.x) / 2, (rh.y + it.y) / 2, 3) },
            )
            val shinVotes = listOf(lk to la, rk to ra).mapNotNull { (k, a) ->
                if (k == null || a == null) return@mapNotNull null
                val skinFrac = img.segmentFraction(k, a, 0.2f, 0.75f, ::isSkin)
                val clothFrac = img.segmentFraction(k, a, 0.2f, 0.75f) { c -> !isSkin(c) && thigh.any { colorDist(c, it) < 85 } }
                skinFrac >= 0.5f || (thigh.isNotEmpty() && clothFrac < 0.25f && skinFrac >= 0.25f)
            }
            if (shinVotes.isNotEmpty()) shorts = shinVotes.count { it } * 2 >= shinVotes.size

            val kneeY = listOfNotNull(lk?.y, rk?.y).takeIf { it.isNotEmpty() }?.average()?.toFloat()
            val bottomRect = if (kneeY != null && kneeY - hipY > hipW * 0.3f) {
                val legH = kneeY - hipY
                val bottomEnd = if (shorts) hipY + legH * 0.55f else kneeY - legH * 0.05f
                RectF(hxMin - hipW * 0.1f, hipY + legH * 0.12f, hxMax + hipW * 0.1f, bottomEnd)
            } else if (hipY < img.h - hipW * 0.3f) {
                RectF(hxMin, hipY + hipW * 0.1f, hxMax, min(img.h.toFloat(), hipY + hipW))
            } else {
                null
            }
            bottomRect?.let { r ->
                img.clusters(r).firstOrNull { !isSkin(it.color) || it.share > 0.6f }?.let {
                    pants = it.color
                    regions += StyleRegion(BodyPart.BOTTOM, r, pants)
                }
            }
            notes += if (shorts) StyleNote.SHORTS else StyleNote.LONG_PANTS

            // ---- shoes ----
            val footRects = listOf(
                Triple(la, lm(PoseLandmark.LEFT_HEEL), lm(PoseLandmark.LEFT_FOOT_INDEX)),
                Triple(ra, lm(PoseLandmark.RIGHT_HEEL), lm(PoseLandmark.RIGHT_FOOT_INDEX)),
            ).mapNotNull { (ankle, heel, toe) ->
                ankle ?: return@mapNotNull null
                val pts = listOfNotNull(ankle, heel, toe)
                val size = max(hipW * 0.18f, pts.maxOf { dist(it, ankle) })
                if (pts.size == 1) {
                    RectF(ankle.x - size, ankle.y + size * 0.1f, ankle.x + size, ankle.y + size * 1.1f)
                } else {
                    RectF(pts.minOf { it.x } - size * 0.25f, pts.minOf { it.y }, pts.maxOf { it.x } + size * 0.25f, pts.maxOf { it.y } + size * 0.35f)
                }
            }
            val shoeSamples = footRects.flatMap { img.collect(it).asList() }.toIntArray()
            val shoes = kmeans(shoeSamples, 3).firstOrNull { !isSkin(it.color) }?.color
            return buildResult(
                base, name, regions + footRects.map { StyleRegion(BodyPart.SHOES, it, shoes ?: base.shoes) }.takeIf { shoes != null }.orEmpty(),
                notes, hair, skin, shirt, stripe, striped, accent, pants,
                shoes ?: ColorUtils.blendARGB(pants, Color.BLACK, 0.55f), shortSleeve, shorts, longHair,
            )
        }

        // Upper body only (selfie): keep a darker version of the top as bottom if we know nothing better.
        notes += StyleNote.UPPER_BODY_ONLY
        return buildResult(
            base, name, regions, notes, hair, skin, shirt, stripe, striped, accent,
            pants, base.shoes, shortSleeve, shorts, longHair,
        )
    }

    private fun buildResult(
        base: Outfit, name: String, regions: List<StyleRegion>, notes: List<StyleNote>,
        hair: Int, skin: Int, shirt: Int, stripe: Int, striped: Boolean, accent: Int,
        pants: Int, shoes: Int, shortSleeve: Boolean, shorts: Boolean, longHair: Boolean,
    ) = StyleResult(
        outfit = base.copy(
            name = name, hair = hair, skin = skin, shirt = shirt, stripe = stripe, striped = striped,
            accent = accent, pants = pants, shoes = shoes,
            shortSleeve = shortSleeve, shorts = shorts, longHair = longHair,
        ),
        regions = regions,
        personDetected = true,
        notes = notes.distinct(),
    )

    // ------------------------------------------------------------------ fallback path

    /** No person found (e.g. a photo of a shirt on a hanger): use the photo's center colors. */
    private fun analyzeWithoutPerson(img: Img, base: Outfit, name: String): StyleResult {
        val center = RectF(img.w * 0.2f, img.h * 0.2f, img.w * 0.8f, img.h * 0.8f)
        val clusters = img.clusters(center, k = 4, requirePerson = false)
        val shirt = clusters.firstOrNull()?.color ?: base.shirt
        val second = clusters.getOrNull(1)
        val striped = second != null && second.share > 0.2f && colorDist(shirt, second.color) > 100 &&
            img.alternations(center, shirt, second.color, requirePerson = false) >= 4
        val accent = clusters.drop(if (striped) 2 else 1).firstOrNull { colorDist(it.color, shirt) > 90 }?.color
            ?: contrastOf(shirt)
        val pants = clusters.drop(1).firstOrNull { luminance(it.color) < luminance(shirt) - 0.15 }?.color
            ?: ColorUtils.blendARGB(shirt, Color.BLACK, 0.45f)
        return StyleResult(
            outfit = base.copy(
                name = name, shirt = shirt, accent = accent, pants = pants,
                stripe = if (striped) second.color else base.stripe, striped = striped,
                shoes = ColorUtils.blendARGB(pants, Color.BLACK, 0.5f),
            ),
            regions = listOf(StyleRegion(BodyPart.OUTFIT, center, shirt)),
            personDetected = false,
            notes = if (striped) listOf(StyleNote.STRIPED) else emptyList(),
        )
    }

    // ------------------------------------------------------------------ pixel helpers

    private class Mask(val values: FloatArray, val w: Int, val h: Int)

    private class Img(bitmap: Bitmap, val mask: Mask?) {
        val w = bitmap.width
        val h = bitmap.height
        val minDim = min(w, h)
        val px = IntArray(w * h).also { bitmap.getPixels(it, 0, w, 0, 0, w, h) }

        fun isPerson(x: Int, y: Int): Boolean {
            val m = mask ?: return true
            val mx = (x.toLong() * m.w / w).toInt().coerceIn(0, m.w - 1)
            val my = (y.toLong() * m.h / h).toInt().coerceIn(0, m.h - 1)
            return m.values[my * m.w + mx] > 0.5f
        }

        fun at(x: Int, y: Int) = px[y.coerceIn(0, h - 1) * w + x.coerceIn(0, w - 1)]

        /** Average of a small patch (reduces JPEG noise). */
        fun patch(cx: Float, cy: Float, r: Int = 2): Int {
            var rs = 0; var gs = 0; var bs = 0; var n = 0
            for (dy in -r..r) for (dx in -r..r) {
                val c = at(cx.toInt() + dx, cy.toInt() + dy)
                rs += Color.red(c); gs += Color.green(c); bs += Color.blue(c); n++
            }
            return Color.rgb(rs / n, gs / n, bs / n)
        }

        /** Pixels inside [rect] (clamped), person-only when a mask exists, capped at ~1500 samples. */
        fun collect(rect: RectF, requirePerson: Boolean = true, fallbackToAll: Boolean = true): IntArray {
            val l = rect.left.toInt().coerceIn(0, w - 1)
            val t = rect.top.toInt().coerceIn(0, h - 1)
            val r = rect.right.toInt().coerceIn(0, w)
            val b = rect.bottom.toInt().coerceIn(0, h)
            if (r - l < 2 || b - t < 2) return IntArray(0)
            val step = max(1, sqrt((r - l).toFloat() * (b - t) / 1500f).toInt())
            val out = ArrayList<Int>()
            var y = t
            while (y < b) {
                var x = l
                while (x < r) {
                    if (!requirePerson || isPerson(x, y)) out += px[y * w + x]
                    x += step
                }
                y += step
            }
            // Mask can miss thin parts (hair tips, shoes): fall back to all pixels.
            if (fallbackToAll && requirePerson && mask != null && out.size < 25) return collect(rect, requirePerson = false)
            return out.toIntArray()
        }

        fun clusters(rect: RectF, k: Int = 3, requirePerson: Boolean = true, fallbackToAll: Boolean = true) =
            kmeans(collect(rect, requirePerson, fallbackToAll), k)

        fun fraction(rect: RectF, requirePerson: Boolean, test: (Int) -> Boolean): Float {
            val l = rect.left.toInt().coerceIn(0, w - 1)
            val t = rect.top.toInt().coerceIn(0, h - 1)
            val r = rect.right.toInt().coerceIn(0, w)
            val b = rect.bottom.toInt().coerceIn(0, h)
            if (r - l < 2 || b - t < 2) return 0f
            var hit = 0; var total = 0
            for (y in t until b step 2) for (x in l until r step 2) {
                total++
                if ((!requirePerson || isPerson(x, y)) && test(px[y * w + x])) hit++
            }
            return if (total == 0) 0f else hit.toFloat() / total
        }

        /** Share of sample points along a→b (from t0 to t1) whose color passes [test]. */
        fun segmentFraction(a: PointF, b: PointF, t0: Float, t1: Float, test: (Int) -> Boolean): Float {
            val n = 8
            var hit = 0
            for (i in 0 until n) {
                val t = t0 + (t1 - t0) * i / (n - 1)
                if (test(patch(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t))) hit++
            }
            return hit.toFloat() / n
        }

        /** Number of color switches between [c1] and [c2] going down the middle of [rect]. */
        fun alternations(rect: RectF, c1: Int, c2: Int, requirePerson: Boolean = true): Int {
            val cx = rect.centerX()
            val t = rect.top.toInt().coerceIn(0, h - 1)
            val b = rect.bottom.toInt().coerceIn(0, h)
            val runs = ArrayList<Pair<Int, Int>>() // (label, length)
            for (y in t until b) {
                if (requirePerson && !isPerson(cx.toInt(), y)) continue
                val c = patch(cx, y.toFloat(), 1)
                val d1 = colorDist(c, c1)
                val d2 = colorDist(c, c2)
                if (min(d1, d2) > 110) continue
                val label = if (d1 <= d2) 1 else 2
                if (runs.isNotEmpty() && runs.last().first == label) {
                    runs[runs.lastIndex] = label to runs.last().second + 1
                } else {
                    runs += label to 1
                }
            }
            // Ignore tiny runs (noise, seams) before counting switches.
            val minRun = max(2, (b - t) / 60)
            val solid = runs.filter { it.second >= minRun }.map { it.first }
            return solid.zipWithNext().count { (a, b2) -> a != b2 }
        }
    }

    data class Cluster(val color: Int, val share: Float)

    companion object {
        /** Deterministic k-means on RGB; returns clusters sorted by size (largest first). */
        fun kmeans(samples: IntArray, k: Int = 3, iterations: Int = 10): List<Cluster> {
            if (samples.isEmpty()) return emptyList()
            val n = samples.size
            val rs = FloatArray(n) { Color.red(samples[it]).toFloat() }
            val gs = FloatArray(n) { Color.green(samples[it]).toFloat() }
            val bs = FloatArray(n) { Color.blue(samples[it]).toFloat() }

            // Farthest-first init starting from the median-brightness sample.
            val order = (0 until n).sortedBy { rs[it] + gs[it] + bs[it] }
            val centers = mutableListOf(floatArrayOf(rs[order[n / 2]], gs[order[n / 2]], bs[order[n / 2]]))
            while (centers.size < k) {
                var best = -1; var bestD = 0f
                for (i in 0 until n) {
                    val d = centers.minOf { c -> sq(rs[i] - c[0]) + sq(gs[i] - c[1]) + sq(bs[i] - c[2]) }
                    if (d > bestD) { bestD = d; best = i }
                }
                if (best < 0 || bestD < 100f) break // all remaining samples are basically the same color
                centers += floatArrayOf(rs[best], gs[best], bs[best])
            }

            val assign = IntArray(n)
            repeat(iterations) {
                for (i in 0 until n) {
                    var bi = 0; var bd = Float.MAX_VALUE
                    centers.forEachIndexed { ci, c ->
                        val d = sq(rs[i] - c[0]) + sq(gs[i] - c[1]) + sq(bs[i] - c[2])
                        if (d < bd) { bd = d; bi = ci }
                    }
                    assign[i] = bi
                }
                val sums = Array(centers.size) { FloatArray(4) }
                for (i in 0 until n) {
                    val s = sums[assign[i]]
                    s[0] += rs[i]; s[1] += gs[i]; s[2] += bs[i]; s[3] += 1f
                }
                sums.forEachIndexed { ci, s ->
                    if (s[3] > 0) centers[ci] = floatArrayOf(s[0] / s[3], s[1] / s[3], s[2] / s[3])
                }
            }
            val counts = IntArray(centers.size).also { c -> assign.forEach { c[it]++ } }
            return centers.indices
                .filter { counts[it] > 0 }
                .map { Cluster(Color.rgb(centers[it][0].toInt(), centers[it][1].toInt(), centers[it][2].toInt()), counts[it].toFloat() / n) }
                .sortedByDescending { it.share }
        }

        /** "Redmean" perceptual distance, 0..~765. */
        fun colorDist(a: Int, b: Int): Double {
            val r1 = Color.red(a); val r2 = Color.red(b)
            val rMean = (r1 + r2) / 2.0
            val dr = (r1 - r2).toDouble()
            val dg = (Color.green(a) - Color.green(b)).toDouble()
            val db = (Color.blue(a) - Color.blue(b)).toDouble()
            return sqrt((2 + rMean / 256) * dr * dr + 4 * dg * dg + (2 + (255 - rMean) / 256) * db * db)
        }

        /** Classic YCrCb skin range, used when the face wasn't visible. */
        fun isSkinTone(c: Int): Boolean {
            val r = Color.red(c); val g = Color.green(c); val b = Color.blue(c)
            val cr = 128 + 0.5 * r - 0.4187 * g - 0.0813 * b
            val cb = 128 - 0.1687 * r - 0.3313 * g + 0.5 * b
            // Skin always has clearly more red than blue; white/grey fabric doesn't.
            return cr in 135.0..180.0 && cb in 85.0..135.0 && abs(r - g) > 10 && r - b > 20
        }

        fun luminance(c: Int) = ColorUtils.calculateLuminance(c)

        /** Collar/belt color that stays visible against the shirt. */
        fun contrastOf(c: Int): Int =
            if (luminance(c) < 0.3) ColorUtils.blendARGB(c, Color.WHITE, 0.55f) else ColorUtils.blendARGB(c, Color.BLACK, 0.4f)

        private fun sq(v: Float) = v * v
        private fun dist(a: PointF, b: PointF) = hypot(a.x - b.x, a.y - b.y)
    }
}

/** Minimal Task -> coroutine bridge (avoids an extra dependency). */
private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
    addOnCanceledListener { cont.cancel() }
}
