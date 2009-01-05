/*
 * Copyright 2006-2009 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peakpicking.peakrecognition.savitzkygolay.peakmodels;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.MzPeak;
import net.sf.mzmine.data.PeakStatus;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.util.Range;

public class TrianglePeakModel implements ChromatographicPeak {

    private double rtRight = -1, rtLeft = -1;
    private double rtMain, intensityMain, alpha, beta;
    public double getArea() {
        // TODO Auto-generated method stub
        return 0;
    }
    public RawDataFile getDataFile() {
        // TODO Auto-generated method stub
        return null;
    }
    public double getHeight() {
        // TODO Auto-generated method stub
        return 0;
    }
    public double getMZ() {
        // TODO Auto-generated method stub
        return 0;
    }
    public int getMostIntenseFragmentScanNumber() {
        // TODO Auto-generated method stub
        return 0;
    }
    public MzPeak getMzPeak(int scanNumber) {
        // TODO Auto-generated method stub
        return null;
    }
    public PeakStatus getPeakStatus() {
        // TODO Auto-generated method stub
        return null;
    }
    public double getRT() {
        // TODO Auto-generated method stub
        return 0;
    }
    public Range getRawDataPointsIntensityRange() {
        // TODO Auto-generated method stub
        return null;
    }
    public Range getRawDataPointsMZRange() {
        // TODO Auto-generated method stub
        return null;
    }
    public Range getRawDataPointsRTRange() {
        // TODO Auto-generated method stub
        return null;
    }
    public int getRepresentativeScanNumber() {
        // TODO Auto-generated method stub
        return 0;
    }
    public int[] getScanNumbers() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
    public ChromatographicPeak fillingPeak(
            ChromatographicPeak originalDetectedShape, double[] params) {

        ConnectedMzPeak[] listMzPeaks = ((ConnectedPeak) originalDetectedShape).getAllMzPeaks();

        double C, factor;

        C = params[0]; // level of excess;
        factor = (1.0f - (Math.abs(C) / 10.0f));

        intensityMain = originalDetectedShape.getHeight() * factor;
        rtMain = originalDetectedShape.getRT();

        rtRight = -1;
        rtLeft = -1;
        calculateBase(listMzPeaks);

        alpha = (double) Math.atan(intensityMain / (rtMain - rtLeft));
        beta = (double) Math.atan(intensityMain / (rtRight - rtMain));

        // Calculate intensity of each point in the shape.
        double t, shapeHeight;
        ConnectedMzPeak newMzPeak = listMzPeaks[0].clone();
        t = listMzPeaks[0].getScan().getRetentionTime();
        shapeHeight = getIntensity(t);
        ((SimpleMzPeak) newMzPeak.getMzPeak()).setIntensity(shapeHeight);
        ConnectedPeak filledPeak = new ConnectedPeak(
                originalDetectedShape.getDataFile(), newMzPeak);

        for (int i = 1; i < listMzPeaks.length; i++) {

            t = listMzPeaks[i].getScan().getRetentionTime();
            shapeHeight = getIntensity(t);

            newMzPeak = listMzPeaks[i].clone();
            ((SimpleMzPeak) newMzPeak.getMzPeak()).setIntensity(shapeHeight);
            filledPeak.addMzPeak(newMzPeak);
        }

        return filledPeak;
    }

    public double getIntensity(double rt) {

        double intensity = 0;
        if ((rt > rtLeft) && (rt < rtRight)) {
            if (rt <= rtMain) {
                intensity = (double) Math.tan(alpha) * (rt - rtLeft);
            }
            if (rt > rtMain) {
                intensity = (double) Math.tan(beta) * (rtRight - rt);
            }
        }

        return intensity;
    }

    private void calculateBase(ConnectedMzPeak[] listMzPeaks) {

        double halfIntensity = intensityMain / 2, intensity = 0, intensityPlus = 0, retentionTime = 0;
        ConnectedMzPeak[] rangeDataPoints = listMzPeaks;

        for (int i = 0; i < rangeDataPoints.length - 1; i++) {

            intensity = rangeDataPoints[i].getMzPeak().getIntensity();
            intensityPlus = rangeDataPoints[i + 1].getMzPeak().getIntensity();
            retentionTime = rangeDataPoints[i].getScan().getRetentionTime();

            if (intensity > intensityMain)
                continue;

            // Left side of the curve
            if (retentionTime < rtMain) {
                if ((intensity <= halfIntensity)
                        && (intensityPlus >= halfIntensity)) {
                    rtLeft = retentionTime;
                    continue;
                }
            }

            // Right side of the curve
            if (retentionTime > rtMain) {
                if ((intensity >= halfIntensity)
                        && (intensityPlus <= halfIntensity)) {

                    rtRight = retentionTime;
                    break;
                }
            }
        }

        if ((rtRight <= -1) && (rtLeft > 0)) {
            double beginning = rangeDataPoints[0].getScan().getRetentionTime();
            double ending = rangeDataPoints[rangeDataPoints.length - 1].getScan().getRetentionTime();
            rtRight = rtMain + ((ending - beginning) / 4.71f);
        }

        if ((rtRight > 0) && (rtLeft <= -1)) {
            double beginning = rangeDataPoints[0].getScan().getRetentionTime();
            double ending = rangeDataPoints[rangeDataPoints.length - 1].getScan().getRetentionTime();
            rtLeft = rtMain - ((ending - beginning) / 4.71f);
        }

        boolean negative = (((rtRight - rtLeft)) < 0);

        if ((negative) || ((rtRight == -1) && (rtLeft == -1))) {
            double beginning = rangeDataPoints[0].getScan().getRetentionTime();
            double ending = rangeDataPoints[rangeDataPoints.length - 1].getScan().getRetentionTime();
            rtRight = rtMain + ((ending - beginning) / 9.42f);
            rtLeft = rtMain - ((ending - beginning) / 9.42f);
        }
    }
    */

}