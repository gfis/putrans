/*  Emulation of putrans.c functional interface
    @(#) $Id: TextConverter.java 566 2010-10-19 16:32:04Z gfis $
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

    /** Upper bound for input buffer */
    protected static final int MAX_BUF = 4096;

    /** Root element tag */
    protected static final String ROOT_TAG        = "html";
    /** Body element tag */
    protected static final String BODY_TAG        = "body";
    /** Byte element tag */
    protected static final String BYTE_TAG        = "bx";
    /** Header element tag */
    protected static final String HEAD_TAG        = "header";
    /** Markup element tag */
    protected static final String MARKUP_TAG      = "markup";
    /** Ruler element tag */
    protected static final String RULER_TAG       = "ruler";
    /** Proportional text line element tag */
    protected static final String PROP_TEXT_TAG   = "propText";
    /** Text line element tag */
    protected static final String TEXT_TAG        = "text";
    /** Word element tag */
    protected static final String WORD_TAG        = "wx";
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
    protected byte[] byteBuffer;
    
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
        setFormatCodes("ibm6788,6788,wheelwriter");
        setDescription("IBM6788 / Wheelwriter");
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
    } // initialize

    /** Emits document text, and writes its characters
     */
    protected void fireContent() {
        if (content.length() > 0) {
            fireCharacters(content.toString());
            content.setLength(0);
        }
    } // fireContent

    /** Emits an arbitrary byte as hexadecimal code
     *  @param ch byte to be output
     */
    protected void fireByte(char ch) {
        fireContent();
        fireEmptyElement(BYTE_TAG, toAttribute(BYTE_TAG, Integer.toHexString(ch)));
    } // fireByte

    /** Emits an arbitrary word (2 LSB bytes) as hexadecimal code
     *  @param word word to be output
     */
    protected void fireWord(int word) {
        fireContent();
        fireEmptyElement(WORD_TAG, toAttribute(WORD_TAG, Integer.toHexString(word)));
    } // fireByte

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
                        case 0x00: 
                            /* ignore nil */
                            break;
                        default:
                            // ch = emap.ebc_asc[ch];
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
            fireLineBreak();
            fireStartRoot(BODY_TAG);
            fireLineBreak();
            while ((len = genRecord.read(byteReader)) >= 0) {
                len = processInput(0, len);
            } // while reading
            fireContent();
            fireEndElement(BODY_TAG);
            fireLineBreak();
            fireEndElement(ROOT_TAG);
            fireLineBreak();
            fireEndDocument();
        } catch (Exception exc) {
            log.error(exc.getMessage(), exc);
        }
        return  result;
    } // generate

    /*===========================*/
    /* Text processing interface */
    /*===========================*/

    /** Status for paragraph end */
    /* parameter 'status' of 'put_bold', 'put_italic' etc. */
    protected static final int ptx_on        = 1;
    protected static final int ptx_off       = 0;
    
    /* Parameter 'status of 'put_line', 'put_space', 'put_hyphen' */
    protected static final int ptx_soft      = 1;
    protected static final int ptx_hard      = 2;
    protected static final int ptx_soft_eol  = 3;
    protected static final int ptx_paragraph = 4;
    protected static final int ptx_normal    = 5;                
    protected static final int ptx_break     = 6; /* similiar to hard */

    /** Emits a newline
     *  @param status one of the codes ptx_soft, ptx_hard and so on.
     */
    protected void putLine(int status) {
        switch (status) {
            case ptx_soft:
                break;
            default:
                content.append("\r\n");
                break;
        } // switch status
    } // putLine

    /** Emits a tab
     */
    protected void putTab() {
        content.append("\t");
    } // putLine

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
            } else if (qName.equals(BYTE_TAG        )) {
                flushLine();
                putLSB(attrs.getValue(BYTE_TAG  ), 1, true);
            } else if (qName.equals(HEAD_TAG        )) {
            } else if (qName.equals(MARKUP_TAG      )) {
                byteWriter.write(0x10);
            } else if (qName.equals(PROP_TEXT_TAG   )) {
                byteWriter.write(0x12);
                putLSB(attrs.getValue(SPACE_ATTR), 1, false);
            } else if (qName.equals(RULER_TAG       )) {
                byteWriter.write(0x80);
            } else if (qName.equals(TEXT_TAG        )) {
                byteWriter.write(0x0d);
                putLSB(attrs.getValue(SPACE_ATTR), 1, false);
            } else if (qName.equals(WORD_TAG        )) {
                flushLine();
                putLSB(attrs.getValue(WORD_TAG  ), 2, true);
            } else if (qName.equals("b"         )) {
                saxBuffer[saxPos ++] = 0x02;
            } else if (qName.equals("u"         )) {
                saxBuffer[saxPos ++] = 0x03;
            } else if (qName.equals("sup"       )) {
                saxBuffer[saxPos ++] = 0x04;
            } else if (qName.equals("sub"       )) {
                saxBuffer[saxPos ++] = 0x05;
            } else if (qName.equals("i"         )) {
                saxBuffer[saxPos ++] = 0x06;
            } else if (qName.equals("shy"       )) {
                if (false) { // problems with 2-byte ISO 6937 accented characters
                saxBuffer[saxPos] = saxBuffer[saxPos - 1];
                saxBuffer[saxPos - 1] = 0x07; // insert before last character
                saxPos ++;
                } else {
                saxBuffer[saxPos ++] = 0x07;
                }
            } else if (qName.equals("tab"           )) {
                saxBuffer[saxPos ++] = 0x08;
            } else if (qName.equals("strike"        )) {
                saxBuffer[saxPos ++] = 0x13;
             } else if (qName.equals("para"         )) {
                saxBuffer[saxPos ++] = 0x1f;
            } else if (qName.equals("nbsp"          )) {
                saxBuffer[saxPos ++] = (byte) 0xa0;
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
                flushLine();
            } else if (qName.equals(HEAD_TAG        )) {
                flushLine();
            } else if (qName.equals(MARKUP_TAG      )) {
                flushLine();
                byteWriter.write(EOS);
            } else if (qName.equals(PROP_TEXT_TAG   )) {
                flushLine();
                byteWriter.write(EOS);
            } else if (qName.equals(RULER_TAG       )) {
                flushLine();
                byteWriter.write(EOS);
            } else if (qName.equals(TEXT_TAG        )) {
                flushLine();
                byteWriter.write(EOS);
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
                    if (chx == '\n' || chx == '\r') {
                        // ignore
                    } else if (chx >= 0x20 && chx <= 0x7e) { // normal printable ASCII character
                        saxBuffer[saxPos ++] = (byte) chx;
                    } else if (chx >= 0x80) { // accented or from table
                        int value = 0; // saxIsoMap.getIsocode(chx);
                        if (value == 0) {
                            saxBuffer[saxPos ++] = (byte) '?';
                        } else if (value >= 0x100) {
                            saxBuffer[saxPos ++] = (byte) (value >> 8);
                            saxBuffer[saxPos ++] = (byte) (value & 0xff);
                        } else {
                            saxBuffer[saxPos ++] = (byte) (value & 0xff);
                        }
                    } else {
                        saxBuffer[saxPos ++] = (byte) chx;
                    }
                    pos ++;
                } // while pos
            } // else ignore characters in unknown elements
        } catch (Exception exc) {
            throw new SAXException(exc.getMessage());
        }
    } // characters

} // TextConverter