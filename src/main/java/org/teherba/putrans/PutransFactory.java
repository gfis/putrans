/*  Selects the applicable converter
    @(#) $Id: PutransFactory.java 966 2012-08-29 07:06:07Z gfis $
    2017-07-22, Georg Fischer: copied from xtrans.PutransFactory

    Usage:
        java -cp dist/putrans.jar org.teherba.putrans.PutransFactory
    Output files:
        src/main/java/org/teherba/putrans/package.html
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
import  org.teherba.xtrans.BaseTransformer;
import  org.teherba.xtrans.XtransFactory;
import  java.io.File;
import  java.io.PrintWriter;
import  java.util.ArrayList;
import  java.util.Iterator;
import  java.util.Properties;
import  java.util.StringTokenizer;
import  org.apache.logging.log4j.Logger;
import  org.apache.logging.log4j.LogManager;

/** Selects a specific converter, and iterates over the descriptions
 *  of all converters and their codes.
 *  Furthermore, it can create a transformation pipeline.
 *  @author Dr. Georg Fischer
 */
public class PutransFactory extends XtransFactory {
    public final static String CVSID = "@(#) $Id: PutransFactory.java 966 2012-08-29 07:06:07Z gfis $";

    /** log4j logger (category) */
    private Logger log;

    /** No-args Constructor. Used for generation and serialization.
     *  Constructs all enabled converters. Their constructors should
     *  not contain any heavy-weight initialization code, since they are
     *  all instantiated here, even if only two of them are really used.
     */
    public PutransFactory() {
        super();
        log        = LogManager.getLogger(PutransFactory.class.getName());
        realPath   = "";
        saxFactory = getSAXFactory();
        try {
            transformers = new ArrayList<BaseTransformer>(64);
            // the order here defines the order in documentation.jsp,
            // should be: "... group by package order by package, name"

            this.enable("putrans.TextConverter");
            this.enable("putrans.conv.IBM6788Converter");
            this.enable("xtrans.general.HexDumpTransformer");
            this.enable("xtrans.general.SeparatedTransformer");
            this.enable("xtrans.XMLTransformer");
        } catch (Exception exc) {
            log.error(exc.getMessage(), exc);
        }
    } // Constructor

    /** Attempts to instantiate the class for some transformer = format
     *  @param transformerName name of the class for the transformer,
     *  without the prefix "org.teherba.".
     *  In {@link XtransFactory}, this method contains the package "xtrans.",
     *  but here, the names to be specified start at 1 level higher
     */
    protected void enable(String transformerName) {
        try {
            BaseTransformer transformer = (BaseTransformer) Class.forName("org.teherba."
                    + transformerName).newInstance();
            if (transformer != null) {
                transformer.initialize();
                // System.err.println(transformerName + " enabled");
                transformers.add(transformer);
            } // != null
        } catch (Exception exc) {
            log.debug(exc.getMessage(), exc);
            // ignore any error silently - this format will not be known
        }
    } // enable

    /** Main program
     *  @param args commandline arguments (none)
     */
    public static void main(String[] args) {
        PutransFactory factory = new PutransFactory();
        Iterator iter = factory.getIterator();
        try {
        } catch (Exception exc) {
            factory.log.error(exc.getMessage(), exc);
        } // try
    } // main

} // PutransFactory
