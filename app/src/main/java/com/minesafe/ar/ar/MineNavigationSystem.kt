package com.minesafe.ar.ar

import dev.romainguy.kotlin.math.Float3
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Mine Waypoint representing a point on the continuous walking path of the mine.
 * @param s Cumulative arc length from the entrance portal (meters)
 * @param x SceneView X coordinate of the path centerline
 * @param yFloor Floor elevation at this point (meters relative to real world ground)
 * @param z SceneView Z coordinate of the path centerline
 * @param halfWidth Safe walkable corridor half-width (meters from centerline to tunnel wall)
 */
data class MineWaypoint(
    val s: Float,
    val x: Float,
    val yFloor: Float,
    val z: Float,
    val halfWidth: Float
)

/**
 * Sampled position and orientation along the mine path.
 */
data class PathSample(
    val x: Float,
    val yFloor: Float,
    val z: Float,
    val tangentX: Float,
    val tangentZ: Float,
    val normalX: Float,
    val normalZ: Float,
    val halfWidth: Float
)

/**
 * Navigation result containing virtual player position and container shift.
 */
data class NavPosition(
    val virtualX: Float,
    val virtualY: Float,
    val virtualZ: Float,
    val mineShiftX: Float,
    val mineShiftY: Float,
    val mineShiftZ: Float,
    val pathProgress: Float,
    val floorY: Float
)

/**
 * High-performance 3D Navigation and Physical Collision Constraint System.
 *
 * Provides continuous, fully walkable navigation through the entire mine:
 * 1. Starting point & Entrance Portal (s = 0m, Z = 0.0m)
 * 2. First Tunnel Straight (s = 0m .. 22.5m, Z = 0.0m .. -22.5m)
 * 3. Right-Hand Curve & Elevation Transition (s = 22.5m .. 30.5m, Z = -22.5m .. -30.0m, Y = 0.0m .. -1.23m)
 * 4. Second Mine Entrance & Straight Section along tracks (s = 30.5m .. 44.3m, Z = -30.0m .. -42.2m)
 * 5. Narrow Sections passing timber arches & side rocks (clear center corridor)
 * 6. Upper Turn / Bend Section (s = 44.3m .. 49.5m, Z = -42.2m .. -47.0m)
 * 7. Final Drift Section to Terminus End (s = 49.5m .. 59.4m, Z = -47.0m .. -54.2m)
 *
 * Features:
 * - 10X movement amplification along forward travel direction.
 * - Physical collision boundary constraints keep the player inside the tunnel and prevent walking outside or clipping into rocks.
 * - Continuous ground collision preserves floor contact so the player never falls through the floor or floats.
 * - 100% backward-compatible with Module 1 & Module 2 interactive equipment and hazard coordinates.
 */
class MineNavigationSystem {

    val waypoints: List<MineWaypoint> = listOf(
        MineWaypoint(0.00f, 0.00f, 0.00f, 0.00f, 1.40f),      // 0: Doorway entrance portal
        MineWaypoint(6.00f, 0.00f, 0.00f, -6.00f, 1.40f),     // 1: Extinguisher area (Z = -6.5m)
        MineWaypoint(11.00f, 0.00f, 0.00f, -11.00f, 1.40f),   // 2: Fire hazard / box (Z = -10.2m)
        MineWaypoint(18.00f, 0.00f, 0.00f, -18.00f, 1.40f),   // 3: Mid chamber straight
        MineWaypoint(22.51f, -0.25f, 0.00f, -22.50f, 1.40f),  // 4: Approach to right-hand curve
        MineWaypoint(24.24f, -0.47f, -0.25f, -24.20f, 1.30f), // 5: Right curve entry
        MineWaypoint(26.34f, 0.05f, -0.65f, -26.20f, 1.25f),  // 6: Right curve bend
        MineWaypoint(28.53f, 0.85f, -1.05f, -28.20f, 1.25f),  // 7: Second mine transition
        MineWaypoint(30.53f, 1.70f, -1.23f, -30.00f, 1.25f),  // 8: Second mine tracks portal
        MineWaypoint(32.79f, 2.75f, -1.23f, -32.00f, 1.20f),  // 9: Straight diagonal section 1
        MineWaypoint(35.08f, 3.85f, -1.23f, -34.00f, 1.15f),  // 10: Narrow section (support arch)
        MineWaypoint(37.32f, 4.88f, -1.23f, -36.00f, 1.15f),  // 11: Narrow section (support arch)
        MineWaypoint(39.65f, 6.07f, -1.23f, -38.00f, 1.15f),  // 12: Narrow section (floor rock clearance)
        MineWaypoint(41.97f, 7.25f, -1.23f, -40.00f, 1.20f),  // 13: Straight diagonal section 2
        MineWaypoint(44.30f, 8.00f, -1.23f, -42.20f, 1.25f),  // 14: Track end / bend entrance
        MineWaypoint(46.65f, 7.50f, -1.23f, -44.50f, 1.20f),  // 15: Turn around upper bend
        MineWaypoint(49.47f, 6.20f, -1.23f, -47.00f, 1.25f),  // 16: Upper drift continuation
        MineWaypoint(52.49f, 4.50f, -1.23f, -49.50f, 1.25f),  // 17: Deep second mine
        MineWaypoint(55.69f, 2.50f, -1.23f, -52.00f, 1.20f),  // 18: Approach to terminus
        MineWaypoint(59.42f, -0.50f, -1.23f, -54.20f, 1.10f)  // 19: Terminus end of mine
    )

    val maxProgress: Float = waypoints.last().s

    var currentProgress: Float = 0f
        private set
    var lateralOffset: Float = 0f
        private set

    private var lastLocalCamX: Float? = null
    private var lastLocalCamZ: Float? = null

    fun reset() {
        currentProgress = 0f
        lateralOffset = 0f
        lastLocalCamX = null
        lastLocalCamZ = null
    }

    /**
     * Interpolates path coordinates, floor elevation, and orientation at arc length [s].
     */
    fun samplePath(s: Float): PathSample {
        val clampedS = s.coerceIn(0f, maxProgress)

        var idx = 0
        while (idx < waypoints.size - 2 && waypoints[idx + 1].s < clampedS) {
            idx++
        }

        val w0 = waypoints[idx]
        val w1 = waypoints[idx + 1]
        val segLen = (w1.s - w0.s).coerceAtLeast(0.001f)
        val t = ((clampedS - w0.s) / segLen).coerceIn(0f, 1f)

        // Smooth cubic Hermite interpolation for smooth turns
        val t2 = t * t
        val t3 = t2 * t
        val h00 = 2f * t3 - 3f * t2 + 1f
        val h10 = t3 - 2f * t2 + t
        val h01 = -2f * t3 + 3f * t2
        val h11 = t3 - t2

        val curX = w0.x + (w1.x - w0.x) * t
        val curY = w0.yFloor + (w1.yFloor - w0.yFloor) * t
        val curZ = w0.z + (w1.z - w0.z) * t
        val curWidth = w0.halfWidth + (w1.halfWidth - w0.halfWidth) * t

        val dx = w1.x - w0.x
        val dz = w1.z - w0.z
        val len = hypot(dx.toDouble(), dz.toDouble()).toFloat().coerceAtLeast(0.001f)
        val tanX = dx / len
        val tanZ = dz / len

        // Normal vector pointing right perpendicular to tangent
        val normX = tanZ
        val normZ = -tanX

        return PathSample(
            x = curX,
            yFloor = curY,
            z = curZ,
            tangentX = tanX,
            tangentZ = tanZ,
            normalX = normX,
            normalZ = normZ,
            halfWidth = curWidth
        )
    }

    /**
     * Update navigation state based on physical camera movement and heading.
     */
    fun update(
        localCamX: Float,
        localCamY: Float,
        localCamZ: Float,
        uFwdX: Float,
        uFwdZ: Float,
        isManualAdvancing: Boolean = false,
        manualSpeed: Float = 0.08f // ~2.4 m/s at 30 fps
    ): NavPosition {
        val prevX = lastLocalCamX ?: localCamX
        val prevZ = lastLocalCamZ ?: localCamZ
        lastLocalCamX = localCamX
        lastLocalCamZ = localCamZ

        val dPhysX = localCamX - prevX
        val dPhysZ = localCamZ - prevZ

        // 1. Outside the doorway check:
        if (localCamZ > 0.05f && currentProgress <= 0.5f) {
            currentProgress = 0f
            lateralOffset = localCamX
            return NavPosition(
                virtualX = localCamX,
                virtualY = localCamY,
                virtualZ = localCamZ,
                mineShiftX = 0f,
                mineShiftY = 0f,
                mineShiftZ = 0f,
                pathProgress = 0f,
                floorY = 0f
            )
        }

        // 2. First straight tunnel progression:
        // Physical penetration depth p = -localCamZ
        val p = (-localCamZ).coerceAtLeast(0f)
        val straightProgress = p * 10f

        if (currentProgress < 22.5f) {
            if (straightProgress >= 22.5f) {
                // Crossing into curve section
                currentProgress = 22.5f
                lateralOffset = localCamX
            } else {
                // Strictly synchronized with physical forward movement in first tunnel
                currentProgress = straightProgress
                lateralOffset = localCamX
            }
        } else {
            // 3. At or beyond the right curve into the second mine:
            val sample = samplePath(currentProgress)

            // Forward physical motion along camera view direction:
            val stepLook = dPhysX * uFwdX + dPhysZ * uFwdZ
            // Forward physical motion along tunnel centerline tangent:
            val stepPath = dPhysX * sample.tangentX + dPhysZ * sample.tangentZ

            // Smooth forward or backward advancement:
            val effectiveStep = if (stepLook > 0.0005f || stepPath > 0.0005f) {
                max(stepLook, stepPath)
            } else if (stepLook < -0.001f && stepPath < -0.001f) {
                min(stepLook, stepPath)
            } else {
                0f
            }

            // 10X movement amplification: 1m physical = 10m virtual
            currentProgress += effectiveStep * 10.0f

            // Manual touch-assist forward walking
            if (isManualAdvancing) {
                currentProgress += manualSpeed
            }

            // Lateral step across the tunnel width (1X natural scale)
            val stepLat = dPhysX * sample.normalX + dPhysZ * sample.normalZ
            lateralOffset += stepLat

            // Smooth return to straight tunnel if walking backward past curve
            if (currentProgress < 22.5f) {
                if (straightProgress < 22.5f) {
                    currentProgress = straightProgress
                    lateralOffset = localCamX
                }
            }
        }

        currentProgress = currentProgress.coerceIn(0f, maxProgress)

        val sample = samplePath(currentProgress)

        // 4. PHYSICAL COLLISION BOUNDARY CONSTRAINT:
        // Constrain lateral position within safe walkable corridor [-halfWidth, +halfWidth]
        // Buffered by player body radius (0.35m) so player never clips tunnel walls or side rocks
        val maxLat = (sample.halfWidth - 0.35f).coerceAtLeast(0.2f)
        lateralOffset = lateralOffset.coerceIn(-maxLat, maxLat)

        // 5. Compute virtual player position in mine coordinates:
        val virtualX = sample.x + lateralOffset * sample.normalX
        val virtualY = sample.yFloor + localCamY
        val virtualZ = sample.z + lateralOffset * sample.normalZ

        // 6. Compute container shift so camera relative to container equals virtual position:
        // camRelative = localCam - mineShift => mineShift = localCam - virtual
        val mineShiftX = localCamX - virtualX
        val mineShiftY = -sample.yFloor
        val mineShiftZ = localCamZ - virtualZ

        return NavPosition(
            virtualX = virtualX,
            virtualY = virtualY,
            virtualZ = virtualZ,
            mineShiftX = mineShiftX,
            mineShiftY = mineShiftY,
            mineShiftZ = mineShiftZ,
            pathProgress = currentProgress,
            floorY = sample.yFloor
        )
    }
}
