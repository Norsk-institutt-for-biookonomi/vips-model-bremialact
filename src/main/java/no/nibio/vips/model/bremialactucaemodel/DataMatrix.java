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

import no.nibio.vips.util.DateMap;

/**
 * @copyright 2016 <a href="http://www.nibio.no/">NIBIO</a>
 * @author Tor-Einar Skog <tor-einar.skog@nibio.no>
 */
public class DataMatrix extends DateMap{
    
    public final static String Q0 = "Q0";
    public final static String TXD = "TXD";
    public final static String UM = "UM";
    public final static String BTG = "BTg";
    /**
     * Enough relative humidity yesterday night? (Rule #1) 1 or 0
     */
    public final static String ERHYN = "ERHYN";
    /**
     * Sporulation yesterday night? (Rule #2) 1 or 0
     */
    public final static String SYN = "SYN";
    /**
     * Enough leaf wetness after sunrise yesterday and before sunrise today? (Rule #3) 1 or 0
     */
    public final static String ELWASYBST = "ELWASYBST";
    /**
     * Max temp between sunrise yesterday and sunrise today (Rule #4)
     */
    public final static String TXASYBST = "TXASYBST";
    /**
     * Max temp hourly
     */
    public final static String TX = "TX";
    
    /**
     * Enough relative humidity last night? (Rule #5) 1 or 0
     */
    public final static String ERHLN = "ERHLN";
    /**
     * Sporulation last night? (Rule #6) 1 or 0
     */
    public final static String SLN = "SLN";
    
    /**
     * Enough leaf wetness after sunrise today (Rule #7)
     */
    public final static String ELWAST = "ELWAST";
    
    /**
     * Max temp today (Rule #8) is TXD
     */
    

}
