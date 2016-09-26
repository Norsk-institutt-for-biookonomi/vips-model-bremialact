/*
 * Copyright (c) 2016 NIBIO <http://www.nibio.no/>. 
 * 
 * This file is part of BremiaLactucaeModel.
 * BremiaLactucaeModel is free software: you can redistribute it and/or modify
 * it under the terms of the NIBIO Open Source License as published by 
 * NIBIO, either version 1 of the License, or (at your option) any
 * later version.
 * 
 * BremiaLactucaeModel is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * NIBIO Open Source License for more details.
 * 
 * You should have received a copy of the NIBIO Open Source License
 * along with BremiaLactucaeModel.  If not, see <http://www.nibio.no/licenses/>.
 * 
 */

package no.nibio.vips.model.bremialactucaemodel;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import no.nibio.vips.entity.ModelConfiguration;
import no.nibio.vips.entity.Result;
import no.nibio.vips.entity.ResultImpl;
import no.nibio.vips.entity.WeatherObservation;
import no.nibio.vips.i18n.I18nImpl;
import no.nibio.vips.model.ConfigValidationException;
import no.nibio.vips.model.Model;
import no.nibio.vips.model.ModelExcecutionException;
import no.nibio.vips.model.ModelId;
import no.nibio.vips.util.CommonNamespaces;
import no.nibio.vips.util.ModelUtil;
import no.nibio.vips.util.WeatherUtil;

/**
 * Weather/rule based forecasting model for Bremua lactucae (NO: Salatbladskimmel)
 * The factors are:
 * <ul>
 *      <li>Relative humidity > 93% consecutively for 4 hours the night before yesterday? [true/false]</li>
 *      <li>Sporulation the night before yesterday? (true/false)</li>
 *      <li>Leaf wetness at ground level for 3 consecutive hours in the period from sunset yesterday to sunrise today? (yes/no)</li>
 *      <li>Maximum temperature yesterday (dec C)</li>
 *      <li>Relative humidity > 93% consecutively for 4 hours last night? [true/false]</li>
 *      <li>Sporulation last night?</li>
 *      <li>Leaf wetness at ground level for 3 consecutive hours in the period from sunset yesterday to sunrise today? (yes/no)</li>
 *      <li>Maximum temperature today (deg C)</li>
 * </ul>
 * @copyright 2016 <a href="http://www.nibio.no/">NIBIO</a>
 * @author Tor-Einar Skog <tor-einar.skog@nibio.no>
 */
public class BremiaLactucaeModel extends I18nImpl implements Model {
    private final static ModelId MODEL_ID = new ModelId("BREMIALACT");
    
    
    private ModelUtil modelUtil;
    private WeatherUtil wUtil;
    private DataMatrix dataMatrix;
    private TimeZone timeZone;
    
    private final static Double NIGHT_RADIATION_THRESHOLD = 15.0;
    private final static Double LEAF_WETNESS_LOWER_THRESHOLD = 48.0;
    private final static Integer LEAF_WETNESS_MINIMUM_CONSECUTIVE_HOURS = 3;
    private final static Double RELATIVE_HUMIDITY_LOWER_THRESHOLD = 93.0;
    private final static Integer RELATIVE_HUMIDITY_MINIMUM_CONSECUTIVE_HOURS = 4;
    private final static Double MAX_TEMP_THRESHOLD = 26.5;
    
    public BremiaLactucaeModel()
    {
        super("no.nibio.vips.model.bremialactucaemodel.texts");
        this.modelUtil = new ModelUtil();
        this.wUtil = new WeatherUtil();
    }
    
    @Override
    public List<Result> getResult() throws ModelExcecutionException {
        //System.out.println(this.dataMatrix.toCSV());
        calculateResults();
        Date currentDate = this.dataMatrix.getFirstDateWithParameterValue(DataMatrix.ERHYN);
        Date endDate = this.dataMatrix.getLastDateWithParameterValue(DataMatrix.ERHYN);
        Calendar cal = Calendar.getInstance(timeZone);
        List<Result> retVal = new ArrayList<>();
        while(currentDate.compareTo(endDate) <= 0)
        {
            Result result = new ResultImpl();
            result.setResultValidTime(currentDate);
            
            // Deciding warning status
            Integer warningStatus = Result.WARNING_STATUS_NO_RISK;
            
            // Infection today?
            if(this.dataMatrix.getParamIntValueForDate(currentDate, DataMatrix.SLN) == 1 // Either sporulation last night
                    || (this.dataMatrix.getParamIntValueForDate(currentDate, DataMatrix.SYN) == 1 // Or sporulation yesterday night...
                        && this.dataMatrix.getParamIntValueForDate(currentDate, DataMatrix.ELWASYBST) == 0 // ..where the spores laid dormant all through until today
                        )
                    ) // Sporulation last night or yesterday night
            {
                if(this.dataMatrix.getParamIntValueForDate(currentDate, DataMatrix.ELWAST) == 1) // Enough leaf wetness today
                {
                    if(this.dataMatrix.getParamDoubleValueForDate(currentDate, DataMatrix.TXD) >= BremiaLactucaeModel.MAX_TEMP_THRESHOLD) // Too hot?
                    {
                        warningStatus = Result.WARNING_STATUS_MINOR_RISK;
                    }
                    else
                    {
                        warningStatus = Result.WARNING_STATUS_HIGH_RISK;
                    }
                }
            }
            
            
            result.setWarningStatus(warningStatus);
            
            retVal.add(result);
            
            // Adding the different parameters to the result
            result.setValue(BremiaLactucaeModel.MODEL_ID.toString(), DataMatrix.ERHYN, String.valueOf(this.dataMatrix.getParamIntValueForDate(currentDate, DataMatrix.ERHYN)));
            result.setValue(BremiaLactucaeModel.MODEL_ID.toString(), DataMatrix.SYN, String.valueOf(this.dataMatrix.getParamIntValueForDate(currentDate, DataMatrix.SYN)));
            result.setValue(BremiaLactucaeModel.MODEL_ID.toString(), DataMatrix.ELWASYBST, String.valueOf(this.dataMatrix.getParamIntValueForDate(currentDate, DataMatrix.ELWASYBST)));
            result.setValue(BremiaLactucaeModel.MODEL_ID.toString(), DataMatrix.TXASYBST, String.valueOf(this.dataMatrix.getParamDoubleValueForDate(currentDate, DataMatrix.TXASYBST)));
            result.setValue(BremiaLactucaeModel.MODEL_ID.toString(), DataMatrix.ERHLN, String.valueOf(this.dataMatrix.getParamIntValueForDate(currentDate, DataMatrix.ERHLN)));
            result.setValue(BremiaLactucaeModel.MODEL_ID.toString(), DataMatrix.SLN, String.valueOf(this.dataMatrix.getParamIntValueForDate(currentDate, DataMatrix.SLN)));
            result.setValue(BremiaLactucaeModel.MODEL_ID.toString(), DataMatrix.ELWAST, String.valueOf(this.dataMatrix.getParamIntValueForDate(currentDate, DataMatrix.ELWAST)));
            result.setValue(CommonNamespaces.NS_WEATHER, DataMatrix.TXD, String.valueOf(this.dataMatrix.getParamDoubleValueForDate(currentDate, DataMatrix.TXD)));
            result.setValue(BremiaLactucaeModel.MODEL_ID.toString(), "TXD_THRESHOLD", String.valueOf(BremiaLactucaeModel.MAX_TEMP_THRESHOLD));
            
            cal.setTime(currentDate);
            cal.add(Calendar.DATE,1);
            currentDate = cal.getTime();
        }
        //System.out.println(this.dataMatrix.toCSV());
        return retVal;
    }

    @Override
    public ModelId getModelId() {
        return BremiaLactucaeModel.MODEL_ID;
    }

    @Override
    public String getModelName() {
        return this.getModelName(Model.DEFAULT_LANGUAGE);
    }

    @Override
    public String getModelName(String language) {
        return this.getText("name", language);
    }

    @Override
    public String getLicense() {
        return "\n" +
            "Copyright (c) 2016 NIBIO <http://www.nibio.no/>. \n" +
            "\n" +
            "This file is part of BremiaLactucaeModel.\n" +
            "BremiaLactucaeModel is free software: you can redistribute it and/or modify\n" +
            "it under the terms of the NIBIO Open Source License as published by \n" +
            "NIBIO, either version 1 of the License, or (at your option) any\n" +
            "later version.\n" +
            "\n" +
            "BremiaLactucaeModel is distributed in the hope that it will be useful,\n" +
            "but WITHOUT ANY WARRANTY; without even the implied warranty of\n" +
            "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\n" +
            "NIBIO Open Source License for more details.\n" +
            "\n" +
            "You should have received a copy of the NIBIO Open Source License\n" +
            "along with BremiaLactucaeModel.  If not, see <http://www.nibio.no/licenses/>.";
    }

    @Override
    public String getCopyright() {
        return "(c) 2016 NIBIO (http://www.nibio.no/). Contact: post@nibio.no";
    }

    @Override
    public String getModelDescription() {
        return this.getModelDescription(Model.DEFAULT_LANGUAGE);
    }

    @Override
    public String getModelDescription(String language) {
        try
        {
            return this.modelUtil.getTextWithBase64EncodedImages(this.getText("description", language), this.getClass());
        }
        catch(IOException ex)
        {
            return this.getText("description", language);
        }
    }

    @Override
    public String getWarningStatusInterpretation() {
        return this.getWarningStatusInterpretation(Model.DEFAULT_LANGUAGE);
    }

    @Override
    public String getWarningStatusInterpretation(String language) {
        return this.getText("statusInterpretation", language);
    }

    @Override
    public String getModelUsage() {
        return this.getModelUsage(Model.DEFAULT_LANGUAGE);
    }

    @Override
    public String getModelUsage(String language) {
        return this.getText("usage", language);
    }

    @Override
    public String getSampleConfig() {
        return  "{\n" +
                "\t\"loginInfo\":{\n" +
                "\t\t\"username\":\"example\",\n" +
                "\t\t\"password\":\"example\"\n" +
                "\t},\n" +
                "\t\"modelId\":\"" + MODEL_ID.toString() + "\",\n" +
                "\t\"configParameters\":{\n" +
                "\t\t\"observations\":[\n" +
                "\t\t{\n" +
                "\t\t\t\t\"timeMeasured\": \"2012-08-20T00:00:00+02:00\",\n" +
                "\t\t\t\t\"elementMeasurementTypeId\":\"TX\",\n" +
                "\t\t\t\t\"logIntervalId\":1,\n" +
                "\t\t\t\t\"value\":13.77\n" +
                "\t\t},\n" +
                "\t\t{\n" +
                "\t\t\t\t\"timeMeasured\": \"2012-08-20T00:00:00+02:00\",\n" +
                "\t\t\t\t\"elementMeasurementTypeId\":\"BTg\",\n" +
                "\t\t\t\t\"logIntervalId\":1,\n" +
                "\t\t\t\t\"value\":48\n" +
                "\t\t},\n" +
                "\t\t{\n" +
                "\t\t\t\t\"timeMeasured\": \"2012-08-20T00:00:00+02:00\",\n" +
                "\t\t\t\t\"elementMeasurementTypeId\":\"Q0\",\n" +
                "\t\t\t\t\"logIntervalId\":1,\n" +
                "\t\t\t\t\"value\":366\n" +
                "\t\t},\n" +
                "\t\t{\n" +
                "\t\t\t\t\"timeMeasured\": \"2012-08-20T00:00:00+02:00\",\n" +
                "\t\t\t\t\"elementMeasurementTypeId\":\"UM\",\n" +
                "\t\t\t\t\"logIntervalId\":1,\n" +
                "\t\t\t\t\"value\":60\n" +
                "\t\t}\n" +
                "\t\t]\n" +
                "\t}\n" +
                "}\n";
    }

    @Override
    public void setConfiguration(ModelConfiguration config) throws ConfigValidationException {
        this.dataMatrix = new DataMatrix();
        ObjectMapper objectMapper = new ObjectMapper();
        this.timeZone = TimeZone.getTimeZone((String) config.getConfigParameter("timeZone"));
        // Getting weather data
        List<WeatherObservation> observations = objectMapper.convertValue(config.getConfigParameter("observations"), new TypeReference<List<WeatherObservation>>(){});
        for(WeatherObservation o:observations)
        {
            this.dataMatrix.setParamDoubleValueForDate(o.getTimeMeasured(), o.getElementMeasurementTypeId(), o.getValue());   
        }
    }

    /**
     * The brain of the whole thing...
     * 
     */
    private void calculateResults() throws ModelExcecutionException {
        Calendar cal = Calendar.getInstance(this.timeZone);
        // Decide start date. This would be the first day with weather data from sunset on two
        // days before. (Sunset time the day before yesterday)
        Date firstTimeWithRH = this.dataMatrix.getFirstDateWithParameterValue(DataMatrix.UM);
        Date firstValidWeatherDataDate = wUtil.normalizeToExactDate(firstTimeWithRH, timeZone);
        Date sunsetTime = this.getSunset(firstValidWeatherDataDate);
        
        // UM not present at sunset. Must move to next day
        if(sunsetTime.compareTo(firstTimeWithRH) < 0)
        {
            cal.setTime(firstValidWeatherDataDate);
            cal.add(Calendar.DATE, 1);
            firstValidWeatherDataDate = cal.getTime();
        }
        cal.setTime(firstValidWeatherDataDate);
        cal.add(Calendar.DATE, 2);
        Date currentDate = cal.getTime();
        // Running the results 
        Date lastTimeWithWeatherData = this.dataMatrix.getLastDateWithParameterValue(DataMatrix.UM);
        cal.setTime(lastTimeWithWeatherData);
        Integer lastDayWithWeatherData = cal.get(Calendar.DAY_OF_YEAR);
        while(currentDate.before(lastTimeWithWeatherData))
        {
            // Day before yesterday
            cal.setTime(currentDate);
            Integer thisDayInYear = cal.get(Calendar.YEAR);
            cal.add(Calendar.DATE, -2);
            Date dayBeforeYesterday = cal.getTime();
            cal.add(Calendar.DATE, 1);
            Date yesterday = cal.getTime();
            cal.add(Calendar.DATE, 2);
            cal.add(Calendar.SECOND, -1);
            Date justBeforeMidnight = cal.getTime();
            Date sunsetDayBeforeYesterday = this.getSunset(dayBeforeYesterday);
            Date sunriseYesterday = this.getSunrise(yesterday);
            Date sunsetYesterday = this.getSunset(yesterday);
            Date sunriseToday = this.getSunrise(currentDate);
            if(sunriseToday == null)
            {
                throw new ModelExcecutionException("Not able to determine sunrise today. Too early?");
            }
            Date endOfDataToday = (thisDayInYear < lastDayWithWeatherData) ? justBeforeMidnight
                    : getDateWithCurrentTime(currentDate);
            
            //System.out.println(currentDate + ": sunsetYesterday=" + sunsetYesterday + ", sunriseToday=" + sunriseToday  );
            
            // Rule #1 & #2 (Sporulation the night before yesterday?)
            Boolean enoughRelativeHumidityYesterdayNight = isEnoughRelativeHumidityInPeriod(sunsetDayBeforeYesterday,sunriseYesterday);
            this.dataMatrix.setParamIntValueForDate(currentDate, DataMatrix.ERHYN, enoughRelativeHumidityYesterdayNight ? 1:0);
            this.dataMatrix.setParamIntValueForDate(currentDate, DataMatrix.SYN, enoughRelativeHumidityYesterdayNight ? 1:0);
            // Rule #3 (Enough LW between sunrise yesterday and sunrise today?
            this.dataMatrix.setParamIntValueForDate(currentDate, DataMatrix.ELWASYBST, isEnoughLeafWetnessInPeriod(sunriseYesterday, sunriseToday) ? 1:0);
            // Rule #4 (Max temp between sunrise yesterday and sunrise today)
            this.dataMatrix.setParamDoubleValueForDate(currentDate, DataMatrix.TXASYBST, getMaxTempForPeriod(sunriseYesterday, sunriseToday));
            // Rule #5 & #6 (Sporulation last night?)
            Boolean enoughRelativeHumidityLastNight = isEnoughRelativeHumidityInPeriod(sunsetYesterday,sunriseToday);
            this.dataMatrix.setParamIntValueForDate(currentDate, DataMatrix.ERHLN, enoughRelativeHumidityLastNight ? 1:0);
            this.dataMatrix.setParamIntValueForDate(currentDate, DataMatrix.SLN, enoughRelativeHumidityLastNight ? 1:0);
            // Rule #7 (Enough leaf wetness after sunrise today)
            this.dataMatrix.setParamIntValueForDate(currentDate, DataMatrix.ELWAST, isEnoughLeafWetnessInPeriod(sunriseToday,endOfDataToday) ? 1:0);
            // Rule #8 (Max temp today)
            this.dataMatrix.setParamDoubleValueForDate(currentDate, DataMatrix.TXD, getMaxTempForPeriod(currentDate, endOfDataToday));
            
            cal.setTime(currentDate);
            cal.add(Calendar.DATE,1);
            currentDate = cal.getTime();
        }
    }

    public Date getSunset(Date theDate) {
        Calendar cal = Calendar.getInstance(timeZone);
        cal.setTime(theDate);
        // Sunset may only appear until between 18:00:00 and midnight(23:00:00)
        for(int theHour = 18; theHour <= 23; theHour++)
        {
            cal.set(Calendar.HOUR_OF_DAY, theHour);
            if(this.dataMatrix.getParamDoubleValueForDate(cal.getTime(), DataMatrix.Q0) <= NIGHT_RADIATION_THRESHOLD)
            {
                return cal.getTime();
            }
        }
        return null;
    }
    
    public Date getSunrise(Date theDate)
    {
        Calendar cal = Calendar.getInstance(timeZone);
        cal.setTime(theDate);
        // Sunrise may only appear until between 00:00:00 and noon(10:00:00)
        for(int theHour = 0; theHour <= 10; theHour++)
        {
            cal.set(Calendar.HOUR_OF_DAY, theHour);
            if(this.dataMatrix.getParamDoubleValueForDate(cal.getTime(), DataMatrix.Q0) >= NIGHT_RADIATION_THRESHOLD)
            {
                return cal.getTime();
            }
        }
        return null;
    }

    private Boolean isEnoughLeafWetnessInPeriod(Date sunsetDayBeforeYesterday, Date sunriseYesterday) {
        
        int consecutiveHours = 0;
        Date currentHour = sunsetDayBeforeYesterday;
        Calendar cal = Calendar.getInstance(this.timeZone);
        while(currentHour.before(sunriseYesterday))
        {
            if(this.dataMatrix.getParamDoubleValueForDate(currentHour, DataMatrix.BTG) >= BremiaLactucaeModel.LEAF_WETNESS_LOWER_THRESHOLD)
            {
                consecutiveHours++;
                if(consecutiveHours == BremiaLactucaeModel.LEAF_WETNESS_MINIMUM_CONSECUTIVE_HOURS)
                {
                    return true;
                }
            }
            else
            {
                consecutiveHours = 0;
            }
            cal.setTime(currentHour);
            cal.add(Calendar.HOUR_OF_DAY, 1);
            currentHour = cal.getTime();
        }
        return false;
    }
    
    private Boolean isEnoughRelativeHumidityInPeriod(Date periodStart, Date periodEnd) {
        
        int consecutiveHours = 0;
        Date currentHour = periodStart;
        Calendar cal = Calendar.getInstance(this.timeZone);
        while(currentHour.before(periodEnd))
        {
            if(this.dataMatrix.getParamDoubleValueForDate(currentHour, DataMatrix.UM) >= BremiaLactucaeModel.RELATIVE_HUMIDITY_LOWER_THRESHOLD)
            {
                consecutiveHours++;
                if(consecutiveHours == BremiaLactucaeModel.RELATIVE_HUMIDITY_MINIMUM_CONSECUTIVE_HOURS)
                {
                    return true;
                }
            }
            else
            {
                consecutiveHours = 0;
            }
            cal.setTime(currentHour);
            cal.add(Calendar.HOUR_OF_DAY, 1);
            currentHour = cal.getTime();
        }
        return false;
    }

    private Double getMaxTempForPeriod(Date startTime, Date endTime) {
        
        Double maxTemp = 0.0;
        Calendar cal = Calendar.getInstance(this.timeZone);
        while(startTime.compareTo(endTime) <= 0)
        {
            Double txHour = this.dataMatrix.getParamDoubleValueForDate(startTime, DataMatrix.TX);
            maxTemp = Math.max(maxTemp, txHour);
            cal.setTime(startTime);
            cal.add(Calendar.HOUR_OF_DAY, 1);
            startTime = cal.getTime();
        }
        return maxTemp;
    }

    /**
     * Returns a given date with same hour and minute as system time ("now")
     * @param theDate
     * @return 
     */
    private Date getDateWithCurrentTime(Date theDate) {
        Calendar cal = Calendar.getInstance(this.timeZone);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        cal.setTime(theDate);
        cal.set(Calendar.HOUR_OF_DAY,hour);
        cal.set(Calendar.MINUTE, minute);
        return cal.getTime();
    }

}
