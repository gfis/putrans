/*  Emulation of putrans.c functional interface
    @(#) $Id: TextConverter.java 566 2010-10-19 16:32:04Z gfis $
    2017-07-25: works for IBM6788
    2017-07-22: Georg Fischer
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
import  org.teherba.xtrans.ByteRecord;
import  org.teherba.xtrans.ByteTransformer;
import  org.xml.sax.Attributes;
import  org.xml.sax.helpers.AttributesImpl;
import  org.xml.sax.SAXException;
import  org.apache.log4j.Logger;

/** This class replaces the functional interface for
 *  text processing systems as it was used by the Pascal and C
 *  implementations of Putrans.
 *
 *  @author Dr. Georg Fischer
 */
public class TextConverter extends ByteTransformer {
    public final static String CVSID = "@(#) $Id: TextConverter.java 566 2010-10-19 16:32:04Z gfis $";

    /** log4j logger (category) */
    private Logger log;

    /** Upper bound for input and SAX buffer */
    protected static final int MAX_BUF = 4096;

    /** Root element tag */
    protected static final String ROOT_TAG        = "html";
    /** Body element tag */
    protected static final String BODY_TAG        = "body";
    /** br element tag */
    protected static final String BR_TAG          = "br";
    /** Head element tag */
    protected static final String HEAD_TAG        = "head";
    /** p element tag */
    protected static final String P_TAG           = "p";
    /** pre element tag */
    protected static final String PRE_TAG         = "pre";

    /** Attribute name for leading spaces */
    protected static final String SPACE_ATTR      = "sp";

    /** byte which indicates the end of string */
    protected static final char EOS = 0;

    /** Element which denotes a line break, for readability/reconstruction only */
    protected static final String NEWLINE_TAG     = "n";
    /** Element tag for hard hyphen */
    protected static final String HARD_HYPHEN_TAG = "hhy";
    /** Element tag for soft hyphen */
    protected static final String SOFT_HYPHEN_TAG = "shy";

    /** number of logical line (terminated by EOS) */
    protected int lineNo;

    /** tag for a text line */
    protected String lineTag;

    /** buffer for values in input stream */
    protected StringBuffer content;

    /* 2-byte pair from ruler line */
    protected int rulerPair;

    /** Buffer for a portion of the input file */
    // protected byte[] byteBuffer;

    /** Record for the reader's buffer */
    protected ByteRecord genRecord;

    /** values of {@link #state} */
    private static final int IN_BRACKET     = 1;
    private static final int IN_TEXT        = 2;

    /** state of finite automaton */
    protected  int  state;

    /** No-args Constructor.
     */
    public TextConverter() {
        super();
        log = Logger.getLogger(TextConverter.class.getName());
        setFormatCodes("text,plain");
        setDescription("Plain Text");
        setFileExtensions("txt");
    } // Constructor

    /** Initializes the (quasi-constant) global structures and variables.
     *  This method is called by the {@link org.teherba.xtrans.XtransFactory} once for the
     *  selected generator and serializer.
     */
    public void initialize() {
        super.initialize();
        content = new StringBuffer(2048);
        lineNo  = 0;
        state   = 0;
        ptx     = new Ptx();
    } // initialize

    /** Emits document text, and writes its characters
     */
    protected void fireContent() {
        if (content.length() > 0) {
            fireCharacters(content.toString());
            content.setLength(0);
        }
    } // fireContent

    /** Processes a portion of the input file
     *  @param start offset where to start/resume scanning
     *  @param trap  offset behind last character to be processed
     *  @return offset behind last character which was processed
     */
    protected int processInput(int start, int trap) {
        char ch; // current character to be processed
        boolean readOff; // whether current character should be consumed
        int ibuf = start;
        while (ibuf < trap) { // process all characters
            readOff = true;
            ch = genRecord.get1(ibuf); // (char) (byteBuffer[ibuf] & 0xff);
            switch (state) {

                case IN_TEXT:
                    switch (ch) {
                        case '\n':
                            content.append(ch);
                            fireContent();
                            break;
                        default:
                            content.append(ch);
                            break;
                    } // switch ch
                    break; // IN_TEXT

                default:
                    log.error("invalid state " + state);
                    break;

            } // switch state
            if (readOff) {
                ibuf ++;
            }
        } // while processing
        return ibuf; // new 'start'
    } // processInput

    /** Transforms from the specified format to XML
     *  @return whether the transformation was successful
     */
    public boolean generate() {
        boolean result = true;
        int len; // length read from 'charReader'
        genRecord = new ByteRecord(MAX_BUF);
        lineNo = 0;
        content = new StringBuffer(MAX_BUF);
        state = IN_TEXT;

        putEntityReplacements();
        try {
            fireStartDocument();
            fireStartRoot(ROOT_TAG);
        //    fireLineBreak();
            fireStartElement(BODY_TAG);
        //    fireLineBreak();
            fireStartElement(PRE_TAG);
        //    fireLineBreak();
            while ((len = genRecord.read(byteReader)) >= 0) {
                len = processInput(0, len);
            } // while reading
            fireContent();
            fireEndElement(PRE_TAG);
        //    fireLineBreak();
            fireEndElement(BODY_TAG);
        //    fireLineBreak();
            fireEndElement(ROOT_TAG);
        //    fireLineBreak();
            fireEndDocument();
        } catch (Exception exc) {
            log.error(exc.getMessage(), exc);
        }
        return  result;
    } // generate

    /*===========================*/
    /* Text processing interface */
    /*===========================*/

    // parameter 'status' of 'put_bold', 'put_italic', put_underline' etc.
    protected static final int ptx_off       = 0;
    protected static final int ptx_on        = 1;
    // Parameter 'status of 'put_line', 'put_space', 'put_hyphen'
    protected static final int ptx_soft      = 1;
    protected static final int ptx_hard      = 2;
    protected static final int ptx_soft_eol  = 3;
    protected static final int ptx_paragraph = 4;
    protected static final int ptx_normal    = 5;
    protected static final int ptx_break     = 6; // similiar to hard
    // Parameter 'status of 'put_align'
    protected static final int ptx_left      = 0; // start aligned (default)
    protected static final int ptx_right     = 1; // end aligned
    protected static final int ptx_centred   = 2; //
    protected static final int ptx_justified = 3; // blocked, left and right aligned


    /** The parameter block for the functional text processing interface
     */
    protected class Ptx {
        public int align;
        public int bold;
        public int underline;
        public Ptx() {
            align     = 0;
            bold      = 0;
            underline = 0;
        }
    } // inner class Ptx

    /** Instance of the parameter block */
    protected Ptx ptx;

    /** Starts or ends bold text
     *  @param status off or on
     */
    protected void put_bold(int status) {
        fireContent();
        switch (status) {
            case 0:
                if (ptx.bold >  0) {
                    fireEndElement("strong");
                }
                break;
            default:
                if (ptx.bold == 0) {
                    fireStartElement("strong");
                }
                break;
        } // switch status
        ptx.bold = status;
    } // put_bold

    /** Puts a special character code
     *  @param code the Unicode character to be output
     *  @param codePage - for compatibility, ignored
     */
    protected void put_char_code(char code, int codePage) {
        content.append(code);
    } // put_char_code

    /** Emits a newline
     *  @param status one of the codes ptx_soft, ptx_hard and so on.
     */
    protected void put_line(int status) {
        switch (status) {
            case ptx_soft:
                break;
            default:
                content.append("\r\n");
            /*
                fireContent();
                fireStartElement("br");
            */
                break;
        } // switch status
    } // put_line

    /** Emits a new page
     *  @param status one of the codes ptx_soft, ptx_hard
     */
    protected void put_page(int status) {
        switch (status) {
            case ptx_soft:
            default:
                content.append("\r\n");
                fireContent();
                AttributesImpl attrs = new AttributesImpl();
                attrs.addAttribute("", "style", "style", "CDATA", "page-break-before: always");
                fireStartElement("span", attrs);
                fireEndElement  ("span");
                break;
        } // switch status
    } // put_page

    /** Emits a soft or hard space
     *  @param status one of the codes ptx_soft, ptx_hard.
     */
    protected void put_space(int status) {
        switch (status) {
            case ptx_soft:
                content.append(" ");
                break;
            default:
                content.append("\u00a0");
                break;
        } // switch status
    } // put_line

    /** Emits a space
     */
    protected void put_tab() {
        content.append("\t");
    } // put_tab

    /** Starts or ends underlined text
     *  @param status off or on with some variant:
     *  <pre>
     *  0 = off,
     *  1 = continuous single on (default),
     *  2 = continuous double,
     *  3 = wordwise   single,
     *  4 = wordwise   double  (same as continuous in RTF)
     *  </pre>
     */
    protected void put_underline(int status) {
        fireContent();
        switch (status) {
            case 0:
                if (ptx.underline >  0) {
                    fireEndElement("u");
                }
                break;
            default:
                if (ptx.underline == 0) {
                    if (status == 1) {
                        fireStartElement("u");
                    } else {
                        AttributesImpl attrs = new AttributesImpl();
                        attrs.addAttribute("", "s", "s", "CDATA", Integer.toString(status));
                        fireStartElement("u", attrs);
                    }
                }
                break;
        } // switch status
        ptx.underline = status;
    } // put_underline

    /*===========================*/
    /* SAX handler for XML input */
    /*===========================*/

    /** buffer for output line */
    private byte[] saxBuffer;
    /** current position in <em>saxBuffer</em> */
    private int saxPos;

    /** currently opened element */
    private String elem;

    /** Terminate and write a logical line
     */
    public void flushLine() {
        try {
            byteWriter.write(saxBuffer, 0, saxPos);
            saxPos = 0;
        } catch (Exception exc) {
            log.error(exc.getMessage(), exc);
        }
    } // flushLine

    /** Receive notification of the beginning of the document.
     */
    public void startDocument() {
        saxBuffer = new byte[MAX_BUF]; // a rather long line
        saxPos = 0;
        elem = "";
    } // startDocument

    /** Receive notification of the end of the document.
     */
    public void endDocument() 
            throws SAXException {
        try {
            byteWriter.write(saxBuffer, 0, saxPos);
            saxPos = 0;
        } catch (Exception exc) {
            throw new SAXException(exc.getMessage());
        }
    } // endDocument

    /** Receive notification of the start of an element.
     *  Looks for the element which contains raw lines.
     *  @param uri The Namespace URI, or the empty string if the element has no Namespace URI
     *  or if Namespace processing is not being performed.
     *  @param localName the local name (without prefix),
     *  or the empty string if namespace processing is not being performed.
     *  @param qName the qualified name (with prefix),
     *  or the empty string if qualified names are not available.
     *  @param attrs the attributes attached to the element.
     *  If there are no attributes, it shall be an empty Attributes object.
     *  @throws SAXException for SAX errors
     */
    public void startElement(String uri, String localName, String qName, Attributes attrs)
            throws SAXException {
        if (namespace.length() > 0 && qName.startsWith(namespace)) {
            qName = qName.substring(namespace.length());
        }
        elem = qName;
        try {
            if (false) {
            } else if (qName.equals(ROOT_TAG        )) {
                // ignore
            } else if (qName.equals(BR_TAG          ) ||
                       qName.equals(P_TAG           )) {
               saxBuffer[saxPos ++] = (byte) '\r';
               saxBuffer[saxPos ++] = (byte) '\n';
            } else {
            }
        } catch (Exception exc) {
            throw new SAXException(exc.getMessage());
        }
    } // startElement

    /** Receive notification of the end of an element.
     *  Looks for the element which contains raw lines.
     *  Terminates the line.
     *  @param uri the Namespace URI, or the empty string if the element has no Namespace URI
     *  or if Namespace processing is not being performed.
     *  @param localName the local name (without prefix),
     *  or the empty string if Namespace processing is not being performed.
     *  @param qName the qualified name (with prefix),
     *  or the empty string if qualified names are not available.
     *  @throws SAXException for SAX errors
     */
    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        if (namespace.length() > 0 && qName.startsWith(namespace)) {
            qName = qName.substring(namespace.length());
        }
        elem = "";
        try {
            if (false) {
            } else if (qName.equals(ROOT_TAG        )) {
            } else {
                // all other elements are empty - ignore their end tags
            }
        } catch (Exception exc) {
            throw new SAXException(exc.getMessage());
        }
    } // endElement

    /** Receive notification of character data inside an element.
     *  @param ch the characters.
     *  @param start the start position in the character array.
     *  @param len the number of characters to use from the character array.
     *  @throws SAXException for SAX errors
     */
    public void characters(char[] ch, int start, int len)
            throws SAXException {
        try {
            if (true) { // inside HEAD_TAG, TEXT_TAG and PROP_TEXT_TAG
                int pos = 0;
                while (pos < len) {
                    char chx = ch[start ++];
                    if (false) {
                    } else {
                        saxBuffer[saxPos ++] = (byte) chx;
                        if (saxPos >= MAX_BUF) {
                            byteWriter.write(saxBuffer, 0, saxPos);
                            saxPos = 0;
                        }
                    }
                    pos ++;
                } // while pos
            } // else ignore characters in unknown elements
        } catch (Exception exc) {
            System.err.println(exc.getMessage());
            throw new SAXException(exc.getMessage());
        }
    } // characters

} // TextConverter
