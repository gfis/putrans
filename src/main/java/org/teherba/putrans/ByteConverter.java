/*  Emulation of putrans.c functional interface
    @(#) $Id: ByteConverter.java 566 2010-10-19 16:32:04Z gfis $
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
import  org.teherba.xtrans.ByteTransformer;
import  org.xml.sax.Attributes;
import  org.xml.sax.SAXException;
import  org.apache.log4j.Logger;

/** This class replaces the functional interface for
 *  text processing systems as used by the Pascal and C 
 *  implementations of Putrans.                    
 *  @author Dr. Georg Fischer
 */
public class ByteConverter extends ByteTransformer {
    public final static String CVSID = "@(#) $Id: ByteConverter.java 566 2010-10-19 16:32:04Z gfis $";

    /** log4j logger (category) */
    private Logger log;

    /** Upper bound for input buffer */
    protected static final int MAX_BUF = 4096;

    /** Root element tag */
    protected static final String ROOT_TAG        = "ibm6788";
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

    /** state of finite automaton */
    protected  int  state;

    /** No-args Constructor.
     */
    public ByteConverter() {
        super();
        log = Logger.getLogger(ByteConverter.class.getName());
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

	/** status for paragraph end */
	/* parameter 'status' of 'put_bold', 'put_italic' etc. */
	protected static final int ptx_on  = 1;
	protected static final int ptx_off = 0;
	
	/* parameter 'status of 'put_line', 'put_space', 'put_hyphen' */
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

} // ByteConverter
