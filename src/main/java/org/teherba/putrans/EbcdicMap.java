/*  EbcdicMap.java - generates the code mappings for IBM's EBCDIC character set
 *  @(#) $Id: EbcdicMap.java 9 2008-09-05 05:21:15Z gfis $
 *  2017-07-22, Georg Fischer
 *
 */
/*
 * Copyright 2006 Dr. Georg Fischer <punctum at punctum dot kom>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.teherba.putrans;
import  org.apache.logging.log4j.Logger;
import  org.apache.logging.log4j.LogManager;

/** Generates the code mappings for IBM's EBCDIC character set.
 *  The code tables are taken from Java's Codepage 1047.
 */

public class EbcdicMap {
    public final static String CVSID = "@(#) $Id: EbcdicMap.java 9 2008-09-05 05:21:15Z gfis $";

    /** log4j logger (category) */
    private Logger log;
    
    /** Array bound for both character tables */
    private static final int MAX_TAB = 256;
    /** Converts from EBCDIC to ASCII  */
    public  static final char[] ebc_asc = new char[MAX_TAB];
    /** Converts from ASCII  to EBCDIC */
    public  static final char[] asc_ebc = new char[MAX_TAB];

    /** Initializes mappings for the ISO 6937 character set.
     */
    public EbcdicMap() {
        log = LogManager.getLogger(EbcdicMap.class.getName());
        initialize();
    } // Constructor 0

    /** Initializes the translation tables. 
     *  See https://stackoverflow.com/questions/368603/convert-string-from-ascii-to-ebcdic-in-java
     */
    public void initialize() {
        byte[] bytes = new byte[MAX_TAB];
        char ind = 0; 
        while (ind < MAX_TAB) {
            bytes[ind] = (byte) (ind & 0xff);
            ind ++;
        } // while ind
        
        try {
            char[] chars = (new String(bytes, "CP1047")).toCharArray();
            ind = 0;
            while (ind < MAX_TAB) {
                char val = chars[ind];
                ebc_asc[ind] = val;
                asc_ebc[val] = ind;
                ind ++;
            } // while ind 2
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    } // initialize


} // EbcdicMap
