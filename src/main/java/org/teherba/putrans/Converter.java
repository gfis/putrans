/*  Commandline tool which converts between various text processing file formats.
 *  @(#) $Id: Converter.java 966 2012-08-29 07:06:07Z gfis $
 *  2017-07-22, Georg Fischer: copied from xtrans.MainConverter
 */
/*
 * Copyright 2017 Dr. Georg Fischer <punctum at punctum dot kom>
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
import  org.teherba.putrans.PutransFactory;
import  org.teherba.xtrans.MainTransformer;
import  org.apache.logging.log4j.Logger;
import  org.apache.logging.log4j.LogManager;

/** This program is a filter which reads a file in some foreign format,
 *  converts it to XML, feeds that into one or more XSLT transformations
 *  (which may be supplied as stylesheet source files or as translets), 
 *  and/or filter classes from this package,
 *  and finally serializes the output to a file in the same or some different 
 *  foreign format. 
 *  <p>
 *  The main method reads processing parameter (formats, filenames, filters,
 *  options) from the command line, or from lines in an input file (behind -f).
 *  @author Dr. Georg Fischer
 */
public class Converter extends MainTransformer { 
    public final static String CVSID = "@(#) $Id: Converter.java 966 2012-08-29 07:06:07Z gfis $";

    /** log4j logger (category) */
    public Logger log;
    
    /** Factory delivering transformers for different input and output file formats */
    // private PutransFactory factory;
  
    /** Constructor
     */
    public Converter() {
        log = LogManager.getLogger(Converter.class.getName());
        // System.out.println(factory.toString());
    } // Constructor 0

    /** Main program, processes the commandline arguments
     *  @param args arguments: -form1 file1 -form2 file2
     */
    public static void main(String args[]) {
        Converter converter = new Converter();
        converter.factory = new PutransFactory();
        converter.processFile(args);
    } // main

} // Converter
