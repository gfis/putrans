/*  Servlet which transforms various file formats to and from XML.
    @(#) $Id: XtransServlet.java 796 2011-09-10 13:58:28Z gfis $
 *  2017-07-22: copied from xtrans
    2017-05-28: javadoc 1.8
    2016-10-14: less imports
    2016-09-14: MultiFormatFactory back to dynamic XtransFactory
    2016-09-06: package xtrans.web; uses BasePage; completely revised
    2011-04-06: XtransFactory -> MultiFormatFactory
    2010-12-13: sqlPretty
    2008-06-25: with view="xslTrans"; textarea input
    2007-11-05: also with LexicalHandler
    2005-07-27, Dr. Georg Fischer
    For the discussion of "enctype=multipart/form-data" c.f.
    http://forum.java.sun.com/thread.jspa?threadID=329408&messageID=1340610

    Content-Disposition: attachment; filename="gfis.gif"
    Content-Type: image/gif
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

package org.teherba.putrans.web;
import  org.teherba.putrans.web.IndexPage;
import  org.teherba.putrans.web.Messages;
import  org.teherba.xtrans.BaseTransformer;
import  org.teherba.xtrans.XMLTransformer;
import  org.teherba.putrans.PutransFactory;
import  org.teherba.common.web.BasePage;
import  org.teherba.common.web.MetaInfPage;
import  java.io.IOException;
import  java.io.StringReader;
import  javax.servlet.ServletConfig;
import  javax.servlet.ServletException;
import  javax.servlet.http.HttpServlet;
import  javax.servlet.http.HttpServletRequest;
import  javax.servlet.http.HttpServletResponse;
import  org.apache.logging.log4j.Logger;
import  org.apache.logging.log4j.LogManager;
import  org.apache.commons.fileupload.FileItem;

/** Servlet which converts between various text processing file formats.
 *  This class is the servlet interface to <em>BaseTransformer</em>,
 *  and ressembles the functionality of the commandline interface.
 *  @author Dr. Georg Fischer
 */
public class PutransServlet extends HttpServlet {
    public final static String CVSID = "@(#) $Id: XtransServlet.java 796 2011-09-10 13:58:28Z gfis $";
    public final static long serialVersionUID = 19470629006L;

    /** URL path to this application */
    private String applPath;
    /** log4j logger (category) */
    private Logger log;
    /** common code and messages for auxiliary web pages */
    private BasePage basePage;
    /** name of this application */
    private static final String APP_NAME = "Xtrans";

    /** Delivers some {@link BaseTransformer} */
    private PutransFactory factory;

    /** Initializes the servlet
     *  @param config configuration data
     */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        log = LogManager.getLogger(PutransServlet.class.getName());
        factory = new PutransFactory();
        basePage = new BasePage(APP_NAME);
        Messages.addMessageTexts(basePage);
    } // init

    /** Creates the response for a HTTP GET request.
     *  @param request fields from the client input form
     *  @param response data to be sent back the user's browser
     *  @throws IOException if an IO error occurs
     */
    public void doGet (HttpServletRequest request, HttpServletResponse response) throws IOException {
        generateResponse(request, response);
    } // doGet

    /** Creates the response for a HTTP POST request.
     *  @param request fields from the client input form
     *  @param response data to be sent back the user's browser
     *  @throws IOException if an IO error occurs
     */
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        generateResponse(request, response);
    } // doPost

    /** Creates the response for a HTTP GET or POST request.
     *  @param request fields from the client input form
     *  @param response data to be sent back the user's browser
     *  @throws IOException if an IO error occurs
     */
    public void generateResponse(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        BaseTransformer generator   = null;
        BaseTransformer serializer  = null;
        if (true) { // try {
            String view = basePage.getFilesAndFields(request, new String[]
                    { "view"    , "index"
                    , "lang"    , "en"
                    , "tool"    , "to"
                    , "nsp"     , ""
                    , "opt"     , ""
                    , "pipeline", "- -mt940 -xsl finance/mt940-camt.052.xsl -xml -"
                    , "format"  , "line"
                    , "enc1"    , "UTF-8"
                    , "enc1"    , "UTF-8"
                    , "intext"  , ""
                    } );
            String language   = basePage.getFormField("lang"    );
            String dir        = basePage.getFormField("tool"    );
            String namespace  = basePage.getFormField("nsp"     );
            String options    = basePage.getFormField("opt"     );
            String pipeline   = basePage.getFormField("pipeline");
            String format     = basePage.getFormField("format"  );
            String enc1       = basePage.getFormField("enc1"    );
            String enc2       = basePage.getFormField("enc1"    );
            String intext     = basePage.getFormField("intext"  );
            FileItem fileItem = basePage.getFormFile(0);
            String infile     = "(specify)";
            if (fileItem != null) {
                infile        = fileItem.getName();
            }

            //-------------------------------------
            // first check any parameters
        /*
            if (false) {
            } else if (",ISO-8859-1,UTF-8,"     .indexOf(("," + encoding.toUpperCase() + ",")) < 0) {
                basePage.writeMessage(request, response, language, new String[]
                        { "401", "enc"      , encoding  } );
       */
            //-------------------------------------
            String resultFormat  = "xml"; // remains fixed

            // then switch for the different views
            if (fileItem != null &&
                    (  ! fileItem.getString().matches("\\s*") && ! intext.equals("")
                    ||   fileItem.getString().matches("\\s*") &&   intext.equals("")
                    )) { // either file or intext must be specified
                basePage.writeMessage(request, response, language, new String[] { "407" } );

            } else if (view.equals("index" )) { // show main dialog
                (new IndexPage()).dialog(request, response, basePage);

            } else if (view.equals("index2")) { // do the main transform
                generator  = factory.getTransformer(format); // try whether the format is valid
                if (generator == null) {
                    basePage.writeMessage(request, response, language, new String[] { "401", "format", format } );
                } else {
                    if (dir.equals("from")) { // interchange source and result formats
                        String temp  = resultFormat; // xml
                        resultFormat = format; // foreign
                        format       = temp; // xml
                        serializer   = generator; // foreign
                        generator    = factory.getTransformer(format); // xml
                    } else {
                        serializer   = factory.getTransformer(resultFormat); // xml
                    }
                    generator .parseOptionString(options);
                    generator .setSourceEncoding(generator .getOption("enc1", "UTF-8")); // should be symmetrical for testing
                    serializer.parseOptionString(options);
                    serializer.setResultEncoding(serializer.getOption("enc2", "UTF-8"));
                    generator .setContentHandler(serializer);
                    generator .setLexicalHandler(serializer);
                    response.setCharacterEncoding(serializer.getResultEncoding());

                    this.doTransform(generator, serializer, fileItem, intext, response);
                } // index page: foreign -> XML or vice versa

            } else if (view.equals("license")
                    || view.equals("manifest")
                    || view.equals("notice")
                    ) {
                (new MetaInfPage()).showMetaInf (request, response, basePage, language, view, this);

            } else {
                basePage.writeMessage(request, response, language, new String[] { "401", "view", view });
            }
    /*
        } catch (Exception exc) {
            log.error(exc.getMessage(), exc);
    */
        }
    } // generateResponse

    /** Runs the transformation.
     *  @param generator parser for the input format
     *  @param serializer generates the output format
     *  @param fileItem handle for an uploaded file
     *  @param intext input String from a form field, overtakes <em>fileItem</em> if non-empty
     *  @param response wher to write the output
     *  @throws IOException if an IO error occurs
     */
    private void doTransform(BaseTransformer generator
            , BaseTransformer serializer
            , FileItem fileItem
            , String intext
            , HttpServletResponse response
            ) throws IOException {
        if (true) { // try {
            if (serializer instanceof XMLTransformer) {
                generator.setMimeType("text/xml"); // is needed sometimes because of "&amp;" multiplication
                response.setHeader("Content-Disposition", "inline; filename=\"" + fileItem.getName() + ".xml\"");
                // to XML
            } else { // from XML
                String name = fileItem.getName();
                name = (name.endsWith(".xml"))
                        ? name.substring(0, name.length() - 4) // remove ".xml"
                        : name + "." + generator.getFileExtension(); // append default extension
                response.setHeader("Content-Disposition", "attachment; filename=\"" + name + "\"");
            } // from XML

            response.setContentType(generator.getMimeType());
            if (generator.isBinaryFormat()) {
                generator.setByteReader(fileItem.getInputStream ());
            } else {
                generator.setCharReader(new StringReader
                        ( intext.matches("\\s*")
                        ? fileItem.getString(generator.getSourceEncoding())
                        : intext
                        ));
            }
            if (serializer.isBinaryFormat()) {
                serializer.setByteWriter(response.getOutputStream());
            } else {
                serializer.setCharWriter(response.getWriter      ());
            }
            generator.generate();
            generator .closeAll();
            serializer.closeAll();
    /*
        } catch (Exception exc) {
            log.error(exc.getMessage(), exc);
    */
        }
    } // doTransform

} // XtransServlet
