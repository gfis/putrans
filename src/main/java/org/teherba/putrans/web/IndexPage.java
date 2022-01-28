/*  IndexPage.java - main web page for Putrans
 *  @(#) $Id: 57d01d0860aef0c2f2783647be70c3c381710c86 $
 *  2017-07-22: copied from xtrans
 *  2017-05-28: javadoc 1.8
 *  2016-09-07, Dr. Georg Fischer: adopted from xslTrans.jsp
 *  2011-04-06: MultiFormatFactory
 *  2008-07-30: svn tests
 *  2008-05-31: with field 'view'
 *  2006-10-13: copied from numword
 */
/*
 * Copyright 2016 Dr. Georg Fischer <punctum at punctum dot kom>
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
package org.teherba.putrans.web;
import  org.teherba.putrans.PutransFactory;
import  org.teherba.common.web.BasePage;
import  org.teherba.xtrans.BaseTransformer;
import  org.teherba.xtrans.XtransFactory;
import  java.io.IOException;
import  java.io.PrintWriter;
import  java.io.Serializable;
import  java.util.Iterator;
import  javax.servlet.http.HttpServletRequest;
import  javax.servlet.http.HttpServletResponse;
import  org.apache.commons.fileupload.FileItem;
import  org.apache.logging.log4j.Logger;
import  org.apache.logging.log4j.LogManager;

/** Xtrans main dialog page
 *  @author Dr. Georg Fischer
 */
public class IndexPage implements Serializable {
    public final static String CVSID = "@(#) $Id: 57d01d0860aef0c2f2783647be70c3c381710c86 $";
    public final static long serialVersionUID = 19470629;

    /** log4j logger (category) */
    private Logger log;

    /** No-args Constructor
     */
    public IndexPage() {
        log      = LogManager.getLogger(IndexPage.class.getName());
    } // Constructor

    /** Output the main dialog page for Xtrans
     *  @param request request with header fields
     *  @param response response with writer
     *  @param basePage refrence to common methods and error messages
     *  @throws IOException if an IO error occurs
     */
    public void dialog(HttpServletRequest request, HttpServletResponse response
            , BasePage basePage
            ) throws IOException {
        if (true) { // try {
            String language   = basePage.getFormField("lang"    );
            String dir        = basePage.getFormField("tool"    );
            String namespace  = basePage.getFormField("nsp"     );
            String options    = basePage.getFormField("opt"     );
            String format     = basePage.getFormField("format"  );
            String enc1       = basePage.getFormField("enc1"    );
            String enc2       = basePage.getFormField("enc1"    );
            String intext     = basePage.getFormField("intext"  );
            FileItem fileItem = basePage.getFormFile(0);
            String infile     = "(specify)";
            if (fileItem != null) {
                infile        = fileItem.getName();
            }

            String target = "xml";
            PrintWriter out = basePage.writeHeader(request, response, language);
            out.write("<title>" + basePage.getAppName() + " Main Page</title>\n");
            out.write("</head>\n<body>\n");

            PutransFactory factory = new PutransFactory();
            String border = "0";
            int index = 0;
            out.write("<!-- dir=\"" + dir + "\", format=\"" + format + "\", target=\""+  target
                    + "\", namespace=\""+ namespace + "\", options=\""+ options + "\" -->\n");
            out.write("<h2>xtrans - Format Transformation to/from XML</h2>\n");
            out.write("<form action=\"servlet\" method=\"post\" enctype=\"multipart/form-data\">\n");
            out.write("    <input type = \"hidden\" name=\"view\" value=\"index2\" />\n");
            out.write("    <table cellpadding=\"8\" border=\"" + border + "\">\n");

            out.write("        <tr valign=\"top\">\n");
            out.write("            <td rowspan=\"2\"><strong>Format</strong><br />\n");
            out.write("                <select name=\"format\" size=\"" + factory.size() + "\">\n");
                                       Iterator iter = factory.getIterator();
                                       index = 0;
                                       while (iter.hasNext()) {
                                           BaseTransformer transformer = (BaseTransformer) iter.next();
                                           String code = transformer.getFirstFormatCode();
                                           out.write("<option value=\"" + code + "\""
                                                   + (code.equals(format) ? " selected" : "" )
                                                   + ">" + code + " - " + transformer.getDescription() + "</option>\n");
                                       } // while iter
            out.write("                </select>\n");
            out.write("            </td>\n");

            out.write("            <td>\n");
                                       IndexPage.writeFormOptions(basePage, out, language, dir
                                            , options, namespace, enc1, enc2, infile, intext);
            out.write("            </td>\n");
            out.write("        </tr>\n");
            out.write("    </table><!-- main layout -->\n");
            out.write("</form>\n");

            basePage.writeTrailer(language, "quest");
    /*
        } catch (Exception exc) {
            log.error(exc.getMessage(), exc);
    */
        }
    } // dialog

    /** Write the common part of the form with its input fields.
     *  <ul>
     *  <li>options</li>
     *  <li>namespace</li>
     *  <li>source encoding</li>
     *  <li>target encoding</li>
     *  <li>name of input file</li>
     *  <li>input field</li>
     *  </ul>
     *  @param basePage refers to common web methods and messages
     *  @param out PrintWriter for response
     *  @param language natural language for messages
     *  @param dir "to" or "from"
     *  @param options string of option for specific format
     *  @param namespace namespace prefix
     *  @param enc1 source encoding
     *  @param enc2 target encoding
     *  @param infile input file specifier
     *  @param intext input string
     *  @throws IOException if an IO error occurs
     */
    public static void writeFormOptions(BasePage basePage, PrintWriter out
            , String language
            , String dir
            , String options
            , String namespace
            , String enc1
            , String enc2
            , String infile
            , String intext
            ) throws IOException {
        String border = "0";
        int index = 0;
        String[] optDir     = new String []
                { "to"          // 0
                , "from"        // 1
                } ;
        String[] enDir      = new String []
                { "to"          // 0
                , "from"        // 1
                } ;
        String[] optTarget  = new String []
                { "xml"         // 0
                } ;
        String[] enTarget   = new String []
                { "XML"         // 0
                } ;
        String[] optEnc     = new String []
                { "UTF-8"       // 0
                , "ISO-8859-1"  // 1
                } ;
        String[] enEnc      = new String []
                { "UTF-8"       // 0
                , "ISO-8859-1"  // 1
                } ;

        out.write("                <table border=\"" + border + "\">\n");
        out.write("                    <tr>\n");
        out.write("                    <td>\n");
        out.write("                        <table border=\"" + border  + "\">\n");
                                           if (dir != null && dir.length() > 0) { // with dir
        out.write("                            <tr>\n");
        out.write("                                <td>\n");
        out.write("                                    <select name=\"dir\" size=\"" +  optDir.length  + "\">\n");
                                                       index = 0;
                                                       while (index < optDir.length) {
                                                           out.write("<option value=\""
                                                                   + optDir[index] + "\""
                                                                   + (optDir[index].equals(dir) ? " selected" : "")
                                                                   + ">"
                                                                   + enDir[index] + "</option>\n");
                                                           index ++;
                                                       } // while index
        out.write("                                    </select>\n");
        out.write("                                </td>\n");
        out.write("                                <td>\n");
        out.write("                                    <strong>XML</strong>"); // target
        out.write("                                </td>\n");
        out.write("                                <td>    \n");
        out.write("                                </td>\n");
        out.write("                            </tr>\n");
                                           } // with dir

        out.write("                            <tr>\n");
        out.write("                                <td colspan=\"3\">\n");
        out.write("                                    Options<br />\n");
        out.write("                                    <input name=\"opt\" maxsize=\"80\" size=\"40\" value=\"" + options + "\" />\n");
        out.write("                                </td>\n");
        out.write("                            </tr>\n");
        out.write("                            <tr>\n");
        out.write("                                <td colspan=\"3\">\n");
        out.write("                                    Namespace<br /> \n");
        out.write("                                    <input name=\"nsp\" maxsize=\"20\" size=\"40\" value=\"" + namespace + "\" />\n");
        out.write("                                </td>\n");
        out.write("                            </tr>\n");

        out.write("                            <tr>\n");
        out.write("                                <td>Source Encoding<br /> \n");
        out.write("                                    <select name=\"enc1\" size=\"" + optEnc.length + "\">\n");
                                                       index = 0;
                                                       while (index < optEnc.length) {
                                                           out.write("<option value=\""
                                                                   + optEnc[index] + "\""
                                                                   + (optEnc[index].equals(enc1) ? " selected" : "")
                                                                   + ">"
                                                                   + enEnc[index] + "</option>\n");
                                                           index ++;
                                                       } // while index
        out.write("                                    </select>\n");
        out.write("                                </td>\n");
        out.write("                                <td>Target Encoding<br /> \n");
        out.write("                                    <select name=\"enc2\" size=\"" + optEnc.length + "\">\n");
                                                       index = 0;
                                                       while (index < optEnc.length) {
                                                           out.write("<option value=\""
                                                                   + optEnc[index] + "\""
                                                                   + (optEnc[index].equals(enc2) ? " selected" : "")
                                                                   + ">"
                                                                   + enEnc[index] + "</option>\n");
                                                           index ++;
                                                       } // while index
        out.write("                                    </select>\n");
        out.write("                                </td>\n");
        out.write("                                <td>    \n");
        out.write("                                </td>\n");
        out.write("                            </tr>\n");

        out.write("                        </table>\n");
        out.write("                    </td>\n");
        out.write("                    <td>\n");
        out.write("                        <p />\n");

        out.write("                    </td>\n");
        out.write("                    </tr>\n");
        out.write("                    <tr>\n");
        out.write("                        <td colspan=\"3\">\n");
        out.write("                            <table border=\"" + border + "\">\n");
        out.write("                                <tr>\n");
        out.write("                                    <td>\n");
        out.write("                                    Name of Input File<br />\n");
        out.write("                                    <input name=\"infile\" type=\"file\" style=\"font-family: Courier, monospace\" \n");
        out.write("                                            maxsize=\"512\" size=\"80\" value=\"" + infile + "\"/> \n");
        out.write("                                    &nbsp;&nbsp;<em><strong>or</strong></em>\n");
        out.write("                                    <br />Input source text (non-binary formats only)<br />\n");
        out.write("                                    <textarea name=\"intext\" cols=\"80\" rows=\"10\">" + intext + "</textarea>\n");
        out.write("                                    <br /><input type=\"submit\" value=\"Transform\" />\n");
        out.write("                                    </td>\n");
        out.write("                                </tr>\n");
        out.write("                            </table>\n");
        out.write("                        </td>\n");
        out.write("                    </tr>\n");
        out.write("                    <tr>\n");
        out.write("                        <td colspan=\"3\">\n");
                                               basePage.writeAuxiliaryLinks(language, "main");
        out.write("                        </td>\n");
        out.write("                    </tr>\n");
        out.write("                </table>\n");
    } // writeFormOptions

} // IndexPage
