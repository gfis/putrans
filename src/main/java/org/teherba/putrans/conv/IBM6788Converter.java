/*  IBM 6788 typewriter, also known as Wheelwriter 5000
    äöüÄÖÜß - Caution: always store this file as UTF-8!
    @(#) $Id: IBM6788Converter.java 566 2010-10-19 16:32:04Z gfis $
    2017-07-22: copied from xtrans.office.text.HitTransformer
    2017-05-28: javadoc 1.8
    2008-03-25, Georg Fischer
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

package org.teherba.putrans.conv;
import  org.teherba.putrans.TextConverter;
import  org.teherba.putrans.EbcdicMap;
import  org.teherba.xtrans.ByteRecord;
import  org.xml.sax.Attributes;
import  org.xml.sax.SAXException;
import  org.apache.log4j.Logger;

/** Transformer for the text files of the IBM 6788 typewriter
 *  on 3.5" DS/DD floppy disks, also known as Wheelwriter 5000
 *  (in cooperation with Lexmark).
 *  Text is in EBCDIC, with escape sequences [2b nn ... nn 2b].
 *  The files are all named DOCUMnnn.TXT, nnn = 001, 002 ...
 *  <p>
 *  <ul>
 *  <li>4 sets of margins for pitch 10, 12, PS, 15</li>
 *  <li>4 sets of tab settings dito</li>
 *  <li>Line spacing 1, 1.5, 2, 3</li>
 *  </ul>
 *
 *  Example dump of a 6788 file:
<pre>
    dump -e ../client.../Originale/d03/DOCUM048

                  3    fsize ??  word1 word2 word3 word4
+   0
     0: 2b d6 40 f3 40 da  2 80  48    5a    34  3 5a      O 3  . ..]. .].
    10: 34  3                                              ...............
    20:                                                   ................ ===
    40:                   d6 2b  2b d4 5a       10 33 13  ......O  M]... .
    50: 63 d4 2b a6 89 99 40 88  96 86 86 85 95 6b 40 84  .M wir hoffen, d
    60: 81 a2 a2 40 85 a2 40 c5  a4 83 88 6b 40 84 85 95  ass es Euch, den
    70: 40 97 85 99 a2 cc 95 93  89 83 88 85 95 40 e4 94   pers.nlichen Um
    80: a2 a3 43 95 84 85 95 40  85 95 a3 a2 97 99 85 83  st.nden entsprec
    90: 88 85 95 84 6b 40 a2 96  a6 85 89 a3 40  6 2b d4  hend, soweit   M
    a0: 5a    20    33 13  3 d4  2b 87 a4 a3 40 87 85 88  ]. . ..M gut geh
    b0: a3 40 a4 95 84 40 c9 88  99 40 c5 a4 83 88 40 a5  t und Ihr Euch v
    c0: 96 95 40 a4 95 a2 85 99  85 94 40 21 7f 21 e2 21  on unserem  " S
...
   2b0: 81 84 89 95 40 a9 a4 a9  a4 a2 85 95 84 85 95 4b  adin zuzusenden.
   2c0: 40  6 2b d4 5a    20     33 13  3 d4 2b  6 2b d4     M]. . ..M   M
   2d0: 5a    20    33 13  3 d4  2b c9 95 84 85 94 40 a6  ]. . ..M Indem w
   2e0: 89 99 40 c5 a4 83 88 40  a6 85 89 a3 85 99 88 89  ir Euch weiterhi
   2f0: 95 40 81 93 93 85 a2 40  c7 a4 a3 85 40 a6 dc 95  n alles Gute w.n

   300: a2 83 88 85 95 6b 40 a5  85 99 82 93 85 89 82 85  schen, verbleibe
   310: 95 40 a6 89 99 40 86 dc  99 40 88 85 a4 a3 85 40  n wir f.r heute
   320: 60 40                                             - ..............
   330:                                                   ................
</pre>
 *  @author Dr. Georg Fischer
 */
public class IBM6788Converter extends TextConverter {
    public final static String CVSID = "@(#) $Id: IBM6788Converter.java 566 2010-10-19 16:32:04Z gfis $";

    /** log4j logger (category) */
    private Logger log;

    /** debugging switch */
    private int debug = 1;

    /** EBCDIC maps */
    private EbcdicMap emap;

    /** values of {@link #state} */
    private static final int IN_BRACKET     = 1;
    private static final int IN_TEXT        = 2;
    private static final int IN_DECOR       = 3;

    /** No-args Constructor.
     */
    public IBM6788Converter() {
        super();
        setFormatCodes("ibm6788,6788,wheelwriter");
        setDescription("IBM 6788 / Wheelwriter");
        setFileExtensions("txt");
    } // Constructor

    /** Escape bracket [2b nn ... nn 2b] */
    private StringBuffer bracket;

    private void dump2BBracket() {
        StringBuffer text = new StringBuffer(512);
        text.append("\r\n");
        String sep = "[";
        int ind = 0;
        while (ind < bracket.length()) {
            text.append(sep);
            sep = " ";
            text.append(String.format("%02x", (int) bracket.charAt(ind)));
            ind ++;
            if (ind % 16 == 0) {
                text.append("\r\n");
            }
        } // while ind
        text.append("]");
        fireComment(text.toString());
    } // dump2BBracket

    /** Evaluates the d6 file header.
     *  Assume that a d6 bracket only occurs at the start of the file.
     *  This first d6 header bracket is always 48 bytes long.
     *  It has the file's number ("nn=")
     *  followed by an LSB word for the file length (-0x48).
     *  At offset +8, there starts a
     *  variable list of words (for margins, tab stops?) which
     *  can be zero, and which are terminated by zeroes.
     *  <pre>
     * [2b d6 40 f1 7e c9 08 80 48 00 84 00 54 03 84 00
     *  54 03 74 01 64 02 00 00 00 00 00 00 00 00 00 00
     *  00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
     *  00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
     *  00 00 00 00 00 00 d6 2b]
     *  </pre>
     */
    private void evalD6FileHeader() {
        StringBuffer header = new StringBuffer(512);
        header.append(" head.6788: ");
        header.append(genRecord.getEBCDICString(2, 3));
        long fsize = genRecord.getLSB(2);
        header.append(", size=");
        header.append(String.format("%5d", fsize + 0x48));
        int ch7    = genRecord.get1();
        header.append(", 0x");
        header.append(String.format("%02x", ch7));
        int ind = 0;
        long [] words = new long[0x20];
        while (ind < 0x1e) { // decode
            words[ind] = genRecord.getLSB(ind * 2 + 8, 2);
            ind ++;
        } // while decode
        int ffw = ind; // first free in 'words'
        while (ffw > 0 && words[ffw - 1] == 0) { // ignore trailing zeroes
            ffw --;
        } // ignore trailing zeroes
        header.append(", words=");
        ind = 0;
        while (ind < ffw) { // decode
            header.append(String.format(" %4d", words[ind]));
            ind ++;
        } // while decode
        header.append("\r\n");
        fireComment(header.toString());
    } // evalD6FileHeader

    /** Emits an unknown character code pair
     *  @param ch4 1st character
     *  @param ch5 2nd character
     */
    private void unknownCode(int ch4, int ch5) {
        content.append(String.format("{code %02x,%02x}", ch4, ch5));
     } // unknownCode

    /** Evaluates an a6 bracket.
     *  Accents (2nd byte) are overprinted on EBCDIC characters (1st byte).
     *  With 08, some microstepping is achieved?
     *
     *  <pre>
     *  [2b a6 09 00 xx yy 09 a6 2b]        undecorated
     *  [2b a6 0b 00 22 d6 22 be 0b a6 2b]  Oacute, bold and underlined
     *    0  1  2  3  4  5  6  7  8  9 10 11
     *  </pre>
     */
    private void evalA6Code() {
        int ch4 = bracket.charAt(4); // EBCDIC accent
        int ch5 = bracket.charAt(5); // EBCDIC character to be accented
        if (bracket.length() > 9) { // when decorated, repeat the code form processInput (not exact)
            ch4 = bracket.charAt(5);
            ch5 = bracket.charAt(7);
            state = IN_DECOR;
            switch ((int) bracket.charAt(4)) {
                case 0x20:
                    put_underline (ptx_on);
                    break;
                case 0x21:
                    put_bold      (ptx_on);
                    break;
                case 0x22:
                    put_bold      (ptx_on);
                    put_underline (ptx_on);
                    break;
                default:
                    state = IN_TEXT; // unknown decoration
                    break;
            } // switch (4)
        } // decorated

        switch (ch5) {
            case 0x08: // ignore, micro stepping ???
                if (ch4 >= 0x20 && ch4 <= 0x2f) {
                } else {
                     unknownCode(ch4, ch5);
                }
                break;
            case 0x5f: // circumflex over EBCDIC
                switch(ch4) {
                    case 0x81: put_char_code('â', 437); break; // acircum
                    case 0xc1: put_char_code('Â', 437); break; // Acircum
                    case 0x85: put_char_code('ê', 437); break; // ecircum
                    case 0xc5: put_char_code('Ê', 437); break; // Ecircum
                    case 0x89: put_char_code('î', 437); break; // icircum
                    case 0xc9: put_char_code('Î', 437); break; // Icircum
                    case 0x96: put_char_code('ô', 437); break; // ocircum
                    case 0xd6: put_char_code('Ô', 437); break; // Ocircum
                    case 0xa4: put_char_code('û', 437); break; // ucircum
                    case 0xe4: put_char_code('Û', 437); break; // Ucircum
                    default:   unknownCode(ch4, ch5);   break;
                }
                break;
            case 0x61: // "/" over EBCDIC
                switch(ch4) {
                    case 0x96: put_char_code('ø', 437); break; // o/
                    case 0xd6: put_char_code('Ø', 437); break; // O/
                    default:   unknownCode(ch4, ch5);   break;
                }
                break;
            case 0x79: // grave over EBCDIC
                switch(ch4) {
                    case 0x81: put_char_code('à', 437); break; // agrave
                    case 0xc1: put_char_code('À', 437); break; // Agrave
                    case 0x85: put_char_code('è', 437); break; // egrave
                    case 0xc5: put_char_code('È', 437); break; // Egrave
                    case 0x89: put_char_code('ì', 437); break; // igrave
                    case 0xc9: put_char_code('Ì', 437); break; // Ugrave
                    case 0x96: put_char_code('ò', 437); break; // ograve
                    case 0xd6: put_char_code('Ò', 437); break; // Ograve
                    case 0xa4: put_char_code('ù', 437); break; // ugrave
                    case 0xe4: put_char_code('Ù', 437); break; // Ugrave
                    default:   unknownCode(ch4, ch5);   break;
                }
                break;
            // 91 would be degree/ring
            case 0x9d: // cedilla under EBCDIC
                switch(ch4) {
                    case 0x83: put_char_code('ç', 437); break; // cedil
                    case 0xc3: put_char_code('Ç', 437); break; // Cedil
                    default:   unknownCode(ch4, ch5);   break;
                }
                break;
            // bc would be macron (overline)
            case 0xbd: // diaresis over EBCDIC
                switch(ch4) {
                    case 0x81: put_char_code('ä', 437); break; // ae
                    case 0xc1: put_char_code('Ä', 437); break; // Ae
                    case 0x85: put_char_code('ë', 437); break; // ediaresis
                    case 0xc5: put_char_code('Ë', 437); break; // Ediaresis
                    case 0x89: put_char_code('ï', 437); break; // idiaresis
                    case 0xc9: put_char_code('Ï', 437); break; // Idiaresis
                    case 0x96: put_char_code('ö', 437); break; // oe
                    case 0xd6: put_char_code('Ö', 437); break; // Oe
                    case 0xa4: put_char_code('ü', 437); break; // ue
                    case 0xe4: put_char_code('Ü', 437); break; // Ue
                    default:   unknownCode(ch4, ch5);   break;
                }
                break;
            case 0xbe: // acute over EBCDIC
                switch(ch4) {
                    case 0x81: put_char_code('á', 437); break; // aacute
                    case 0xc1: put_char_code('Á', 437); break; // Aacute
                    case 0x85: put_char_code('é', 437); break; // eacute
                    case 0xc5: put_char_code('É', 437); break; // Eacute
                    case 0x89: put_char_code('í', 437); break; // iacute
                    case 0xc9: put_char_code('Í', 437); break; // Iacute
                    case 0x96: put_char_code('ó', 437); break; // oacute
                    case 0xd6: put_char_code('Ó', 437); break; // Oacute
                    case 0xa4: put_char_code('ú', 437); break; // uacute
                    case 0xe4: put_char_code('Ú', 437); break; // Uacute
                    default:   unknownCode(ch4, ch5);   break;
                }
                break;
            } // switch(ch5)
        } // evalA6Code

    /** Evaluates a a7 bracket (title ?) at the beginning of the file.
     *  The next byte is the length ("19" below) of the whole bracket,
     *  and that length is repeated at the end.
     *  In between, there are EBCDIC characters.
     *  <pre>
     *  [2b a7 19 40 c1 d9 d8 e4 c9 e3 c5 c3 4b 40 c1 e2
     *   e3 e4 d9 c9 c1 e2 19 a7 2b]
     *  </pre>
     */
    private void evalA7Title() {
        StringBuffer text = new StringBuffer(512);
        text.append(" title.6788");
        int len = bracket.charAt(2);
        text.append(String.format("[%d]: ", len));
        int ind = 3;
        while (ind < len - 3) {
            text.append(emap.ebc_asc[bracket.charAt(ind)]);
            ind ++;
        } // while ind
        text.append("\r\n");
        fireComment(text.toString());
    } // evalA7Title

    /** Evaluates an escape sequence and emits the appropriate formatting.
     *  Currently recognized are:
     *  <pre>
     *  [2b a6 09 00 xx yy 09 a6 2b]        overprinting
     *  [2b a7 15 40 7f d9 85 89 a2 85 82 85 99 89 83 88
     *   a3 7f 15 a7 2b]                    title text in quotes? 7f = \"
     *  [2b c2 e0 01 c2 2b]                 tab, 2nd word is varying c2=B
     *  [2b c3 80 01 c3 2b]                 decimal tab? c3=C
     *  [2b d4 84 00 20 00 33 13 00 d4 2b]  put_line(ptx.soft)
     *  [2b d5 6c 00 00 00 d5 2b]           ???
     *  [2b d6 ...                          file header with margin and tab settings ???
     *  </pre>
     */
    private void evalBracket () {
        int code = bracket.charAt(1);
        switch (code) {
            case 0xa6:
                evalA6Code(); // special character codes
                break;
            case 0xa7:
                evalA7Title();
                dump2BBracket();
                fireLineBreak();
                break;
            case 0xc2: // left ???
            case 0xc3: // right or decimal ???
                // put_tab();
                break;
            case 0xd4:
                put_line(ptx_soft);
                break;
            case 0xd6:
                evalD6FileHeader(); // assume that it occurs only once at the beginning of the file
                dump2BBracket();
                fireLineBreak();
                break;
            default:
                dump2BBracket();
                break;
        } // switch (1)
        bracket.setLength(0);
    } // evalBracket

    /** Position (count) of current printable character */
    protected int curPos;
    /** Whether bold is active */
    private int bold_state;
    /** Whether underline is active */
    private int ul_state;

    /** Initializes the (quasi-constant) global structures and variables.
     *  This method is called by the {@link org.teherba.xtrans.XtransFactory} once for the
     *  selected generator and serializer.
     */
    public void initialize() {
        super.initialize();
        log        = Logger.getLogger(IBM6788Converter.class.getName());
        emap       = new EbcdicMap();
        bracket    = new StringBuffer(512);
    } // initialize


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
                        case 0x06:
                            put_line      (ptx_paragraph);
                            break;
                        case 0x07:
                            put_page      (ptx_hard);
                            break;
                        case 0x15:
                            put_line      (ptx_hard);
                            break;
                        case 0x20:
                            put_underline (ptx_on);
                            break;
                        case 0x21:
                            put_bold      (ptx_on);
                            break;
                        case 0x22:
                            put_bold      (ptx_on);
                            put_underline (ptx_on);
                            break;
                        case 0x28:
                            put_space     (ptx_hard);
                            break;
                        case 0x29:
                        case 0x2a:
                            // ???
                            break;
                        case 0x2b: // start of bracket
                            fireContent();
                            bracket.setLength(0);
                            bracket.append(ch);
                            state = IN_BRACKET;
                            break;
                        case 0x2c: // ???
                        case 0x2d:
                        case 0x2e: // oth/DOCUM002.txt
                        case 0x2f:
                            // for block adjustment ???, together with 2b a6 bracket; bra/DOCUM009.TXT
                            // put_space     (ptx_soft);
                            break;
                        case 0x30:
                            // put_align(ptx_centred); start centred ???
                            break;
                        case 0x31:
                            // put_align(ptx_left); end   centred ???
                            break;
                        case 0x33:
                            // tra/DOCUM007.txt ???, no line break
                            break;
                        case 0x34:
                            // next is comma for decimal alignment ???; others/DOCUM006.txt
                            break;
                        case 0x37:
                            put_space     (ptx_soft); // ???
                            break;
                        default:
                            ch = emap.ebc_asc[ch];
                            content.append(ch);
                            if (ptx.bold > 0 || ptx.underline > 0) {
                                state = IN_DECOR;
                            }
                            break;
                    } // switch ch
                    break; // IN_TEXT

                case IN_BRACKET: // during a 2b bracket
                    bracket.append(ch);
                    if (ch == 0x2b) { // trailing - end of bracket
                        state = IN_TEXT; // may be modified in evalBracket for decoration
                        content.setLength(0);
                        if (debug > 0) {
                            // dump2BBracket();
                        }
                        evalBracket();
                    } // trailing
                    break; // IN_BRACKET

                case IN_DECOR: // when bold or underline was on
                    state = IN_TEXT;
                    switch (ch) {
                        case 0x20:
                            if (ptx.bold > 0) {
                                put_bold      (ptx_off);
                                put_underline (ptx_on);
                            }
                            break;
                        case 0x21:
                            if (ptx.underline > 0) {
                                put_bold      (ptx_on);
                                put_underline (ptx_off);
                            }
                            break;
                        case 0x22:
                            put_bold      (ptx_on);
                            put_underline (ptx_on);
                            break;
                        default: // switch all decoration off
                            put_bold     (ptx_off);
                            put_underline(ptx_off);
                            readOff = false;
                            break;
                    } // switch ch
                    break; // IN_BRACKET

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

} // IBM6788Converter
