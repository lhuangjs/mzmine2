/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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
package net.sf.mzmine.modules.peaklistmethods.identification.formulapredictionpeaklist;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.openscience.cdk.formula.MolecularFormulaGenerator;
import org.openscience.cdk.formula.MolecularFormulaRange;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

import com.google.common.collect.Range;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.IonizationType;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.MassList;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimplePeakIdentity;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.ResultFormula;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.restrictions.elements.ElementalHeuristicChecker;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.restrictions.rdbe.RDBERestrictionChecker;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopepatternscore.IsotopePatternScoreCalculator;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopepatternscore.IsotopePatternScoreParameters;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopeprediction.IsotopePatternCalculator;
import net.sf.mzmine.modules.peaklistmethods.msms.msmsscore.MSMSScore;
import net.sf.mzmine.modules.peaklistmethods.msms.msmsscore.MSMSScoreCalculator;
import net.sf.mzmine.modules.peaklistmethods.msms.msmsscore.MSMSScoreParameters;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.FormulaUtils;

public class FormulaPredictionPeakListTask extends AbstractTask {

    private List<ResultFormula> ResultingFormulas;
    private Logger logger = Logger.getLogger(this.getClass().getName());
    private Range<Double> massRange;
    private MolecularFormulaRange elementCounts;
    private MolecularFormulaGenerator generator;
    private IonizationType ionType;
    private double searchedMass;
    private int charge;
    private PeakList peakList;
    private boolean checkIsotopes, checkMSMS, checkRatios, checkRDBE;
    private ParameterSet isotopeParameters, msmsParameters, ratiosParameters,
            rdbeParameters;
    private MZTolerance mzTolerance;
    private String message;
    private int totalRows, finishedRows;

    /**
     *
     * @param parameters
     * @param peakList
     * @param peakListRow
     * @param peak
     */
    FormulaPredictionPeakListTask(PeakList peakList, ParameterSet parameters) {

        /*
         * searchedMass = parameters.getParameter(
         * FormulaPredictionPeakListParameters.neutralMass).getValue();
         */
        this.peakList = peakList;
        charge = parameters
                .getParameter(FormulaPredictionPeakListParameters.charge)
                .getValue();
        ionType = (IonizationType) parameters
                .getParameter(FormulaPredictionPeakListParameters.ionization)
                .getValue();
        mzTolerance = parameters
                .getParameter(FormulaPredictionPeakListParameters.mzTolerance)
                .getValue();
        elementCounts = parameters
                .getParameter(FormulaPredictionPeakListParameters.elements)
                .getValue();

        checkIsotopes = parameters
                .getParameter(FormulaPredictionPeakListParameters.isotopeFilter)
                .getValue();
        isotopeParameters = parameters
                .getParameter(FormulaPredictionPeakListParameters.isotopeFilter)
                .getEmbeddedParameters();

        checkMSMS = parameters
                .getParameter(FormulaPredictionPeakListParameters.msmsFilter)
                .getValue();
        msmsParameters = parameters
                .getParameter(FormulaPredictionPeakListParameters.msmsFilter)
                .getEmbeddedParameters();

        checkRDBE = parameters
                .getParameter(
                        FormulaPredictionPeakListParameters.rdbeRestrictions)
                .getValue();
        rdbeParameters = parameters
                .getParameter(
                        FormulaPredictionPeakListParameters.rdbeRestrictions)
                .getEmbeddedParameters();

        checkRatios = parameters
                .getParameter(
                        FormulaPredictionPeakListParameters.elementalRatios)
                .getValue();
        ratiosParameters = parameters
                .getParameter(
                        FormulaPredictionPeakListParameters.elementalRatios)
                .getEmbeddedParameters();

        message = "Formula Prediction";
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public double getFinishedPercentage() {
        if (totalRows == 0)
            return 0.0;
        return (double) finishedRows / (double) totalRows;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        return message;
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {

        setStatus(TaskStatus.PROCESSING);

        totalRows = peakList.getNumberOfRows();

        for (PeakListRow row : peakList.getRows()) {

            if (row.getPeakIdentities().length > 0) {
                continue;
            }

            this.ResultingFormulas = new ArrayList<ResultFormula>();

            this.searchedMass = (row.getAverageMZ() - ionType.getAddedMass())
                    * charge;

            message = "Formula prediction for " + MZmineCore.getConfiguration()
                    .getMZFormat().format(searchedMass);

            massRange = mzTolerance.getToleranceRange(searchedMass);

            IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
            generator = new MolecularFormulaGenerator(builder,
                    massRange.lowerEndpoint(), massRange.upperEndpoint(),
                    elementCounts);

            IMolecularFormula cdkFormula;
            while ((cdkFormula = generator.getNextFormula()) != null) {
                // Mass is ok, so test other constraints
                checkConstraints(cdkFormula, row);

            }

            if (isCanceled())
                return;

            for (ResultFormula f : this.ResultingFormulas) {
                SimplePeakIdentity newIdentity = new SimplePeakIdentity(
                        f.getFormulaAsString());
                row.addPeakIdentity(newIdentity, false);
            }

            finishedRows++;

        }

        if (isCanceled())
            return;

        logger.finest("Finished formula search for all the peaks");

        setStatus(TaskStatus.FINISHED);

    }

    private void checkConstraints(IMolecularFormula cdkFormula,
            PeakListRow peakListRow) {

        // Check elemental ratios
        if (checkRatios) {
            boolean check = ElementalHeuristicChecker.checkFormula(cdkFormula,
                    ratiosParameters);
            if (!check) {
                return;
            }
        }

        Double rdbeValue = RDBERestrictionChecker.calculateRDBE(cdkFormula);

        // Check RDBE condition
        if (checkRDBE && (rdbeValue != null)) {
            boolean check = RDBERestrictionChecker.checkRDBE(rdbeValue,
                    rdbeParameters);
            if (!check) {
                return;
            }
        }

        // Calculate isotope similarity score
        IsotopePattern detectedPattern = peakListRow.getBestIsotopePattern();
        IsotopePattern predictedIsotopePattern = null;
        Double isotopeScore = null;
        if ((checkIsotopes) && (detectedPattern != null)) {

            String stringFormula = MolecularFormulaManipulator
                    .getString(cdkFormula);

            String adjustedFormula = FormulaUtils.ionizeFormula(stringFormula,
                    ionType, charge);

            final double isotopeNoiseLevel = isotopeParameters
                    .getParameter(
                            IsotopePatternScoreParameters.isotopeNoiseLevel)
                    .getValue();

            final double detectedPatternHeight = detectedPattern
                    .getHighestDataPoint().getIntensity();

            final double minPredictedAbundance = isotopeNoiseLevel
                    / detectedPatternHeight;

            predictedIsotopePattern = IsotopePatternCalculator
                    .calculateIsotopePattern(adjustedFormula,
                            minPredictedAbundance, charge,
                            ionType.getPolarity());

            isotopeScore = IsotopePatternScoreCalculator.getSimilarityScore(
                    detectedPattern, predictedIsotopePattern,
                    isotopeParameters);

            final double minScore = isotopeParameters
                    .getParameter(
                            IsotopePatternScoreParameters.isotopePatternScoreThreshold)
                    .getValue();

            if (isotopeScore < minScore) {
                return;
            }

        }

        // MS/MS evaluation is slowest, so let's do it last
        Double msmsScore = null;
        Feature bestPeak = peakListRow.getBestPeak();
        RawDataFile dataFile = bestPeak.getDataFile();
        Map<DataPoint, String> msmsAnnotations = null;
        int msmsScanNumber = bestPeak.getMostIntenseFragmentScanNumber();

        if ((checkMSMS) && (msmsScanNumber > 0)) {
            Scan msmsScan = dataFile.getScan(msmsScanNumber);
            String massListName = msmsParameters
                    .getParameter(MSMSScoreParameters.massList).getValue();
            MassList ms2MassList = msmsScan.getMassList(massListName);
            if (ms2MassList == null) {
                setStatus(TaskStatus.ERROR);
                setErrorMessage("The MS/MS scan #" + msmsScanNumber
                        + " in file " + dataFile.getName()
                        + " does not have a mass list called '" + massListName
                        + "'");
                return;
            }

            MSMSScore score = MSMSScoreCalculator.evaluateMSMS(cdkFormula,
                    msmsScan, msmsParameters);

            double minMSMSScore = msmsParameters
                    .getParameter(MSMSScoreParameters.msmsMinScore).getValue();

            if (score != null) {
                msmsScore = score.getScore();
                msmsAnnotations = score.getAnnotation();

                // Check the MS/MS condition
                if (msmsScore < minMSMSScore) {
                    return;
                }
            }

        }

        // Create a new formula entry
        final ResultFormula resultEntry = new ResultFormula(cdkFormula,
                predictedIsotopePattern, rdbeValue, isotopeScore, msmsScore,
                msmsAnnotations);

        // Add the new formula entry
        ResultingFormulas.add(resultEntry);

    }

    @Override
    public void cancel() {
        super.cancel();

        // We need to cancel the formula generator, because searching for next
        // candidate formula may take a looong time
        if (generator != null) {
            generator.cancel();
        }

    }
}
