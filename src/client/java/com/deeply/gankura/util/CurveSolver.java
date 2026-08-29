package com.deeply.gankura.util;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * パーティクルの軌跡から、その先にある地点を求める。
 *
 * Hypixel は軌跡を 3次の曲線で描いているが、
 * 途中までしかパーティクルが出ないので、得られた点に同じ次数の式を
 * 当てはめてから、始点の傾きから求めたパラメータで終点を引く。
 *
 * NoFrills の CurveSolver を参考にしている。
 */
public class CurveSolver {

    // 当てはめる式の次数。係数は4つ
    private static final int DEGREE = 3;
    private static final int TERMS = DEGREE + 1;
    // この数を超えてから解く。次数より多くないと式が定まらない
    private static final int MIN_POINTS = TERMS;
    // 直前の点からこれ以上離れているパーティクルは、別の軌跡とみなして入れない
    private static final double MAX_GAP = 5.0;
    // これより近ければ同じ場所とみなす(ブロック)
    private static final double SAME_POINT = 1.0E-4;

    private final List<Vec3> points = new ArrayList<>();
    private Vec3 startPos;
    private Vec3 solved;

    /** 軌跡を取り直す。レーダーを使った瞬間に呼ぶ */
    public void start(Vec3 eyePos) {
        points.clear();
        startPos = eyePos;
    }

    /** 直前の点から離れすぎていないか。別のパーティクルを拾わないため */
    public boolean isConnected(Vec3 pos) {
        Vec3 last = points.isEmpty() ? startPos : points.get(points.size() - 1);
        return last != null && last.distanceTo(pos) <= MAX_GAP;
    }

    public void addPoint(Vec3 pos) {
        // 同じ場所のパーティクルが何度も届く。
        // 点の番号を曲線上の位置として使うので、重なりを入れると式が壊れる
        if (!points.isEmpty() && points.get(points.size() - 1).distanceTo(pos) < SAME_POINT) return;

        points.add(pos);
        if (points.size() > MIN_POINTS) solved = solve();
    }

    /** 推測した地点。まだ求まっていなければ null */
    public Vec3 solved() {
        return solved;
    }

    public void clear() {
        points.clear();
        startPos = null;
        solved = null;
    }

    /**
     * 推測地点だけを忘れる。軌跡は残す。
     *
     * 点が少ないうちは当てはめが粗く、手元のすぐそばを指すことがある。
     * ここで軌跡ごと捨てると、続きのパーティクルを拾えなくなる
     */
    public void forgetSolved() {
        solved = null;
    }

    /**
     * 軌跡に式を当てはめ、終点を求める。
     *
     * 始点の傾き(1次の係数)から、終点にあたる t を逆算する。
     * 最後に y を少し下げて、輪の中心の高さに合わせる
     */
    private Vec3 solve() {
        double[][] coefficients = new double[3][];
        for (int axis = 0; axis < 3; axis++) {
            coefficients[axis] = fitAxis(axis);
            if (coefficients[axis] == null) return solved;
        }

        double dx = coefficients[0][1] / 3;
        double dy = coefficients[1][1] / 3;
        double dz = coefficients[2][1] / 3;
        double end = endParameter(dx, dy, dz);

        double[] result = new double[3];
        double term = 1;
        for (int i = 0; i < TERMS; i++) {
            for (int axis = 0; axis < 3; axis++) {
                result[axis] += coefficients[axis][i] * term;
            }
            term *= end;
        }
        return new Vec3(result[0], result[1] - 0.5, result[2]);
    }

    // 始点の傾きから、終点にあたる t を求める
    private static double endParameter(double x, double y, double z) {
        return 7 / (Math.sqrt(9 * y * y + 7 * (x * x + y * y + z * z)) - 3 * y);
    }

    /**
     * 1つの軸について、最小二乗で係数を求める。
     *
     * 点の番号を t として y = a0 + a1 t + a2 t^2 + a3 t^3 に当てはめる。
     * 正規方程式をそのままガウスの消去法で解くので、行列の逆は作らない
     */
    private double[] fitAxis(int axis) {
        double[][] normal = new double[TERMS][TERMS + 1];
        for (int i = 0; i < points.size(); i++) {
            double[] powers = new double[TERMS];
            powers[0] = 1;
            for (int p = 1; p < TERMS; p++) powers[p] = powers[p - 1] * i;

            double value = axisValue(points.get(i), axis);
            for (int r = 0; r < TERMS; r++) {
                for (int c = 0; c < TERMS; c++) normal[r][c] += powers[r] * powers[c];
                normal[r][TERMS] += powers[r] * value;
            }
        }
        return solveLinear(normal);
    }

    private static double axisValue(Vec3 point, int axis) {
        return axis == 0 ? point.x : axis == 1 ? point.y : point.z;
    }

    // ガウスの消去法。解けなければ null
    private static double[] solveLinear(double[][] matrix) {
        for (int col = 0; col < TERMS; col++) {
            int pivot = col;
            for (int row = col + 1; row < TERMS; row++) {
                if (Math.abs(matrix[row][col]) > Math.abs(matrix[pivot][col])) pivot = row;
            }
            if (Math.abs(matrix[pivot][col]) < 1.0E-9) return null;

            double[] swap = matrix[col];
            matrix[col] = matrix[pivot];
            matrix[pivot] = swap;

            for (int row = 0; row < TERMS; row++) {
                if (row == col) continue;
                double factor = matrix[row][col] / matrix[col][col];
                for (int c = col; c <= TERMS; c++) matrix[row][c] -= factor * matrix[col][c];
            }
        }

        double[] result = new double[TERMS];
        for (int i = 0; i < TERMS; i++) {
            result[i] = matrix[i][TERMS] / matrix[i][i];
            if (!Double.isFinite(result[i])) return null;
        }
        return result;
    }
}
