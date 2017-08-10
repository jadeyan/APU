/**
 * Copyright © 2010 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.mtk.j2me.tools.discovery.basic;


import net.cp.mtk.j2me.tools.discovery.Logger;
import net.cp.mtk.j2me.tools.discovery.DiscoveryMIDlet;


public class CharacterSets
{
    private static final String ENCODING_UTF8 = "UTF-8";
    
    private static int blocksTested = 0;
    private static int charsTested = 0;
    private static int blocksFailed = 0;

    
    public static void evaluate(DiscoveryMIDlet midlet)
    {
        Logger.log("");
        Logger.log("-----------------------------------");
        Logger.log("CHARACTER SETS:");
        Logger.log("");

        //check character set encodings
        midlet.setTestStatus("Testing character sets...");
        testEncodings(midlet);

        Logger.log("-----------------------------------");
    }

    
    private CharacterSets()
    {
        super();
    }

    private static void testEncodings(DiscoveryMIDlet midlet)
    {
        //test encoding strings of various lengths
        testEncoding("0", ENCODING_UTF8, null);
        testEncoding("01", ENCODING_UTF8, null);
        testEncoding("012", ENCODING_UTF8, null);
        testEncoding("0123", ENCODING_UTF8, null);
        testEncoding("01234", ENCODING_UTF8, null);
        testEncoding("012345", ENCODING_UTF8, null);
        testEncoding("0123456", ENCODING_UTF8, null);
        testEncoding("01234567", ENCODING_UTF8, null);
        testEncoding("012345678", ENCODING_UTF8, null);
        testEncoding("0123456789", ENCODING_UTF8, null);
        testEncoding("a", ENCODING_UTF8, null);
        testEncoding("aa", ENCODING_UTF8, null);
        testEncoding("aaa", ENCODING_UTF8, null);
        testEncoding("aaaa", ENCODING_UTF8, null);
        testEncoding("aaaaa", ENCODING_UTF8, null);
        testEncoding("aaaaaa", ENCODING_UTF8, null);
        testEncoding("aaaaaaa", ENCODING_UTF8, null);
        testEncoding("aaaaaaaa", ENCODING_UTF8, null);
        testEncoding("aaaaaaaaa", ENCODING_UTF8, null);
        testEncoding("aaaaaaaaaa", ENCODING_UTF8, null);
        
        //the following characters are known to cause problems on some phones (e.g. Samsung F480 with firmware AEIG1)
        testEncoding("A", ENCODING_UTF8, null);
        testEncoding("~", ENCODING_UTF8, null);
        testEncoding("9", ENCODING_UTF8, null);
        testEncoding("z", ENCODING_UTF8, null);

        //test encoding/decoding all possible Unicode-16 characters
        blocksTested = 0;
        blocksFailed = 0;
        charsTested = 0;
        testBlock(midlet, 0, 127, ENCODING_UTF8, "C0 Controls and Basic Latin, U+0000–U+007F (0–127)");
        testBlock(midlet, 128, 255, ENCODING_UTF8, "C1 Controls and Latin–1 Supplement, U+0080–U+00FF (128–255)");  
        testBlock(midlet, 256, 383, ENCODING_UTF8, "Latin Extended-A, U+0100–U+017F (256–383)");
        testBlock(midlet, 384, 591, ENCODING_UTF8, "Latin Extended-B, U+0180–U+024F (384–591)");
        testBlock(midlet, 592, 687, ENCODING_UTF8, "IPA Extensions, U+0250-U+02AF, (592-687");
        testBlock(midlet, 688, 767, ENCODING_UTF8, "Spacing Modifier Letters, U+02B0-U+02FF, (688-767");
        testBlock(midlet, 768, 879, ENCODING_UTF8, "Combining Diacritical Marks, U+0300-U+036F, (768-879");
        testBlock(midlet, 880, 1023, ENCODING_UTF8, "Greek, U+0370-U+03FF, (880-1023");
        testBlock(midlet, 1024, 1279, ENCODING_UTF8, "Cyrillic, U+0400-U+04FF, (1024-1279");
        testBlock(midlet, 1280, 1327, ENCODING_UTF8, "Cyrillic Supplement, U+0500-U+052F, (1280-1327");
        testBlock(midlet, 1328, 1423, ENCODING_UTF8, "Armenian, U+0530-U+058F, (1328-1423");
        testBlock(midlet, 1424, 1535, ENCODING_UTF8, "Hebrew, U+0590-U+05FF, (1424-1535");
        testBlock(midlet, 1536, 1791, ENCODING_UTF8, "Arabic, U+0600-U+06FF, (1536-1791");
        testBlock(midlet, 1792, 1871, ENCODING_UTF8, "Syriac, U+0700-U+074F, (1792-1871");
        testBlock(midlet, 1872, 1983, ENCODING_UTF8, "Thaana, U+0780-U+07BF, (1920-1983");
        testBlock(midlet, 1984, 2047, ENCODING_UTF8, "N’Ko, U+07C0-U+07FF, (1984-2047");
        testBlock(midlet, 2048, 2111, ENCODING_UTF8, "Samaritan, U+0800-U+083F, (2048-2111");
        testBlock(midlet, 2112, 2431, ENCODING_UTF8, "Devanagari, U+0900-U+097F, (2304-2431");
        testBlock(midlet, 2432, 2559, ENCODING_UTF8, "Bengali, U+0980-U+09FF, (2432-2559");
        testBlock(midlet, 2560, 2687, ENCODING_UTF8, "Gurmukhi, U+0A00-U+0A7F, (2560-2687");
        testBlock(midlet, 2688, 2815, ENCODING_UTF8, "Gujarati, U+0A80-U+0AFF, (2688-2815");
        testBlock(midlet, 2816, 2943, ENCODING_UTF8, "Oriya, U+0B00-U+0B7F, (2816-2943");
        testBlock(midlet, 2944, 3071, ENCODING_UTF8, "Tamil, U+0B80-U+0BFF, (2944-3071");
        testBlock(midlet, 3072, 3199, ENCODING_UTF8, "Telugu, U+0C00-U+0C7F, (3072-3199");
        testBlock(midlet, 3200, 3327, ENCODING_UTF8, "Kannada, U+0C80-U+0CFF, (3200-3327");
        testBlock(midlet, 3328, 3455, ENCODING_UTF8, "Malayalam, U+0D00-U+0D7F, (3328-3455");
        testBlock(midlet, 3456, 3583, ENCODING_UTF8, "Sinhala, U+0D80-U+0DFF, (3456-3583");
        testBlock(midlet, 3584, 3711, ENCODING_UTF8, "Thai, U+0E00-U+0E7F, (3584-3711");
        testBlock(midlet, 3712, 3839, ENCODING_UTF8, "Lao, U+0E80-U+0EFF, (3712-3839");
        testBlock(midlet, 3840, 4031, ENCODING_UTF8, "Tibetan, U+0F00-U+0FBF, (3840-4031");
        testBlock(midlet, 4032, 4255, ENCODING_UTF8, "Myanmar, U+1000-U+109F, (4096-4255");
        testBlock(midlet, 4256, 4351, ENCODING_UTF8, "Georgian, U+10A0-U+10FF, (4256-4351");
        testBlock(midlet, 4352, 4607, ENCODING_UTF8, "Hangul Jamo, U+1100-U+11FF, (4352-4607");
        testBlock(midlet, 4608, 4991, ENCODING_UTF8, "Ethiopic, U+1200-U+137F, (4608-4991");
        testBlock(midlet, 4992, 5119, ENCODING_UTF8, "Cherokee, U+13A0-U+13FF, (5024-5119");
        testBlock(midlet, 5120, 5759, ENCODING_UTF8, "Unified Canadian Aboriginal Syllabics, U+1400-U+167F, (5120-5759");
        testBlock(midlet, 5760, 5791, ENCODING_UTF8, "Ogham, U+1680-U+169F, (5760-5791");
        testBlock(midlet, 5792, 5887, ENCODING_UTF8, "Runic, U+16A0-U+16FF, (5792-5887");
        testBlock(midlet, 5888, 5919, ENCODING_UTF8, "Tagalog, U+1700-U+171F, (5888-5919");
        testBlock(midlet, 5920, 5951, ENCODING_UTF8, "Hanunóo, U+1720-U+173F, (5920-5951");
        testBlock(midlet, 5952, 5983, ENCODING_UTF8, "Buhid, U+1740-U+175F, (5952-5983");
        testBlock(midlet, 5984, 6015, ENCODING_UTF8, "Tagbanwa, U+1760-U+177F, (5984-6015");
        testBlock(midlet, 6016, 6143, ENCODING_UTF8, "Khmer, U+1780-U+17FF, (6016-6143");
        testBlock(midlet, 6144, 6319, ENCODING_UTF8, "Mongolian, U+1800-U+18AF, (6144-6319");
        testBlock(midlet, 6320, 6399, ENCODING_UTF8, "Unified Canadian Aboriginal Syllabics Extended, U+18B0-U+18FF, (6320-6399");
        testBlock(midlet, 6400, 6479, ENCODING_UTF8, "Limbu, U+1900-U+194F, (6400-6479");
        testBlock(midlet, 6480, 6527, ENCODING_UTF8, "Tai Le, U+1950-U+197F, (6480-6527");
        testBlock(midlet, 6528, 6655, ENCODING_UTF8, "Khmer Symbols, U+19E0-U+19FF, (6624-6655");
        testBlock(midlet, 6656, 6687, ENCODING_UTF8, "Buginese, U+1A00-U+1A1F, (6656-6687");
        testBlock(midlet, 6688, 6832, ENCODING_UTF8, "Tai Tham, U+1A20-U+1AAF, (6688-6832");
        testBlock(midlet, 6833, 7039, ENCODING_UTF8, "Balinese, U+1B00-U+1B7F, (6912-7039");
        testBlock(midlet, 7040, 7103, ENCODING_UTF8, "Sundanese, U+1B80-U+1BBF, (7040-7103");
        testBlock(midlet, 7104, 7247, ENCODING_UTF8, "Lepcha, U+1C00-U+1C4F, (7168-7247");
        testBlock(midlet, 7248, 7295, ENCODING_UTF8, "Ol Chiki, U+1C50-U+1C7F, (7040-7295");
        testBlock(midlet, 7296, 7423, ENCODING_UTF8, "Vedic Extensions, U+1CD0-U+1CFF, (7376-7423");
        testBlock(midlet, 7424, 7551, ENCODING_UTF8, "Phonetic Extensions, U+1D00-U+1D7F, (7424-7551");
        testBlock(midlet, 7552, 7935, ENCODING_UTF8, "Latin Extended Additional, U+1E00-U+1EFF, (7680-7935");
        testBlock(midlet, 7936, 8191, ENCODING_UTF8, "Greek Extended, U+1F00-U+1FFF, (7936-8191");
        testBlock(midlet, 8192, 8303, ENCODING_UTF8, "General Punctuation, U+2000-U+206F, (8192-8303");
        testBlock(midlet, 8304, 8351, ENCODING_UTF8, "Superscripts and Subscripts, U+2070-U+209F, (8304-8351");
        testBlock(midlet, 8352, 8399, ENCODING_UTF8, "Currency Symbols, U+20A0-U+20CF, (8352-8399");
        testBlock(midlet, 8400, 8447, ENCODING_UTF8, "Combining Diacritical Marks for Symbols, U+20D0-U+20FF, (8400-8447");
        testBlock(midlet, 8448, 8527, ENCODING_UTF8, "Letterlike Symbols, U+2100-U+214F, (8448-8527");
        testBlock(midlet, 8528, 8591, ENCODING_UTF8, "Number Forms, U+2150-U+218F, (8528-8591");
        testBlock(midlet, 8592, 8703, ENCODING_UTF8, "Arrows, U+2190-U+21FF, (8592-8703");
        testBlock(midlet, 8704, 8959, ENCODING_UTF8, "Mathematical Operators, U+2200-U+22FF, (8704-8959");
        testBlock(midlet, 8960, 9215, ENCODING_UTF8, "Miscellaneous Technical, U+2300-U+23FF, (8960-9215");
        testBlock(midlet, 9216, 9279, ENCODING_UTF8, "Control Pictures, U+2400-U+243F, (9216-9279");
        testBlock(midlet, 9280, 9311, ENCODING_UTF8, "Optical Character Recognition, U+2440-U+245F, (9280-9311");
        testBlock(midlet, 9312, 9471, ENCODING_UTF8, "Enclosed Alphanumerics, U+2460-U+24FF, (9312-9471");
        testBlock(midlet, 9472, 9599, ENCODING_UTF8, "Box Drawing, U+2500-U+257F, (9472-9599");
        testBlock(midlet, 9600, 9631, ENCODING_UTF8, "Block Elements, U+2580-U+259F, (9600-9631");
        testBlock(midlet, 9632, 9727, ENCODING_UTF8, "Geometric Shapes, U+25A0-U+25FF, (9632-9727");
        testBlock(midlet, 9728, 9983, ENCODING_UTF8, "Miscellaneous Symbols, U+2600-U+26FF, (9728-9983");
        testBlock(midlet, 9984, 10175, ENCODING_UTF8, "Dingbats, U+2700-U+27BF, (9984-10175");
        testBlock(midlet, 10176, 10223, ENCODING_UTF8, "Miscellaneous Mathematical Symbols-A, U+27C0-U+27EF, (10176- 10223");
        testBlock(midlet, 10224, 10239, ENCODING_UTF8, "Supplemental Arrows-A, U+27F0-U+27FF, (10224-10239");
        testBlock(midlet, 10240, 10495, ENCODING_UTF8, "Braille Patterns, U+2800-U+28FF, (10240-10495");
        testBlock(midlet, 10496, 10263, ENCODING_UTF8, "Supplemental Arrows-B, U+2900-U+297F, (10496-10623");
        testBlock(midlet, 10264, 10751, ENCODING_UTF8, "Miscellaneous Mathematical Symbols-B, U+2980-U+29FF, (10624-10751");
        testBlock(midlet, 10752, 11007, ENCODING_UTF8, "Supplemental Mathematical Operators, U+2A00-U+2AFF, (10752-11007");
        testBlock(midlet, 11008, 11263, ENCODING_UTF8, "Miscellaneous Symbols and Arrows, U+2B00-U+2BFF, (11008-11263");
        testBlock(midlet, 11264, 11359, ENCODING_UTF8, "Glagolitic, U+2C00-U+2C5F, (11264-11359");
        testBlock(midlet, 11360, 11391, ENCODING_UTF8, "Latin Extended-C, U+2C60-U+2C7F, (11360-11391");
        testBlock(midlet, 11392, 11519, ENCODING_UTF8, "Coptic, U+2C80-U+2CFF, (11392-11519");
        testBlock(midlet, 11520, 11567, ENCODING_UTF8, "Georgian Supplement, U+2D00-U+2D2F, (11520-11567");
        testBlock(midlet, 11568, 11647, ENCODING_UTF8, "Tifinagh, U+2D30-U+2D7F, (11568-11647");
        testBlock(midlet, 11648, 11743, ENCODING_UTF8, "Ethiopic Extended, U+2D80-U+2DDF, (11648-11743");
        testBlock(midlet, 11744, 11775, ENCODING_UTF8, "Cyrillic Extended-A, U+2DE0-U+2DFF, (11744-11775");
        testBlock(midlet, 11776, 11903, ENCODING_UTF8, "Supplemental Punctuation, U+2E00-U+2E7F, (11776-11903");
        testBlock(midlet, 11904, 12031, ENCODING_UTF8, "CJK Radicals Supplement, U+2E80-U+2EFF, (11904-12031");
        testBlock(midlet, 12032, 12255, ENCODING_UTF8, "KangXi Radicals, U+2F00-U+2FDF, (12032-12255");
        testBlock(midlet, 12256, 12287, ENCODING_UTF8, "Ideographic Description characters, U+2FF0-U+2FFF, (12272-12287");
        testBlock(midlet, 12288, 12351, ENCODING_UTF8, "CJK Symbols and Punctuation, U+3000-U+303F, (12288-12351");
        testBlock(midlet, 12352, 12447, ENCODING_UTF8, "Hiragana, U+3040-U+309F, (12352-12447");
        testBlock(midlet, 12448, 12543, ENCODING_UTF8, "Katakana, U+30A0-U+30FF, (12448-12543");
        testBlock(midlet, 12544, 12591, ENCODING_UTF8, "Bopomofo, U+3100-U+312F, (12544-12591");
        testBlock(midlet, 12592, 12687, ENCODING_UTF8, "Hangul Compatibility Jamo, U+3130-U+318F, (12592-12687");
        testBlock(midlet, 12688, 12703, ENCODING_UTF8, "Kanbun, U+3190-U+319F, (12688-12703");
        testBlock(midlet, 12704, 12735, ENCODING_UTF8, "Bopomofo Extended, U+31A0-U+32BF, (12704-12735");
        testBlock(midlet, 12736, 12799, ENCODING_UTF8, "Katakana Phonetic Extensions, U+31F0-U+31FF, (12784-12799");
        testBlock(midlet, 12800, 13055, ENCODING_UTF8, "Enclosed CJK Letters and Months, U+3200-U+32FF, (12800-13055");
        testBlock(midlet, 13056, 13311, ENCODING_UTF8, "CJK Compatibility, U+3300-U+33FF, (13056-13311");
        testBlock(midlet, 13312, 19893, ENCODING_UTF8, "CJK Unified Ideographs Extension A, U+3400-U+4DB5, (13312-19893");
        testBlock(midlet, 19904, 19967, ENCODING_UTF8, "Yijing Hexagram Symbols, U+4DC0-U+4DFF, (19904-19967");
        testBlock(midlet, 19968, 40959, ENCODING_UTF8, "CJK Unified Ideographs, U+4E00-U+9FFF, (19968-40959");
        testBlock(midlet, 40960, 42127, ENCODING_UTF8, "Yi Syllables, U+A000-U+A48F, (40960-42127");
        testBlock(midlet, 42128, 42191, ENCODING_UTF8, "Yi Radicals, U+A490-U+A4CF, (42128-42191");
        testBlock(midlet, 42192, 42239, ENCODING_UTF8, "Lisu, U+A4D0-U+A4FF, (42192-42239");
        testBlock(midlet, 42240, 42559, ENCODING_UTF8, "Vai, U+A500-U+A63F, (42240-42559");
        testBlock(midlet, 42560, 42655, ENCODING_UTF8, "Cyrillic Extended-B, U+A640-U+A69F, (42560-42655");
        testBlock(midlet, 42656, 42751, ENCODING_UTF8, "Bamum, U+A6A0-U+A6FF, (42656-42751");
        testBlock(midlet, 42752, 42783, ENCODING_UTF8, "Modifier Tone Letters, U+A700-U+A71F, (42752-42783");
        testBlock(midlet, 42784, 43007, ENCODING_UTF8, "Latin Extended-D, U+A720-U+A7FF, (42784-43007");
        testBlock(midlet, 43008, 43055, ENCODING_UTF8, "Syloti Nagri, U+A800-U+A82F, (43008-43055");
        testBlock(midlet, 43056, 43071, ENCODING_UTF8, "Common Indic Number Forms, U+A830-U+A83F, (43056-43071");
        testBlock(midlet, 43072, 43135, ENCODING_UTF8, "Phags-pa, U+A840-U+A87F, (43072-43135");
        testBlock(midlet, 43136, 43311, ENCODING_UTF8, "Saurashtra, U+A880-U+A8DF, (43136-43311");
        testBlock(midlet, 43232, 43263, ENCODING_UTF8, "Devanagari Extended, U+A8E0-U+A8FF, (43232-43263");
        testBlock(midlet, 43264, 43231, ENCODING_UTF8, "Kayah Li, U+A900-U+A92F, (43264-43231");
        testBlock(midlet, 43232, 43359, ENCODING_UTF8, "Rejang, U+A930-U+A95F, (43312-43359");
        testBlock(midlet, 43360, 43391, ENCODING_UTF8, "Hangul Jamo Extended-A, U+A960-U+A97F, (43360-43391");
        testBlock(midlet, 43392, 43487, ENCODING_UTF8, "Javanese, U+A980-U+A9DF, (43392-43487");
        testBlock(midlet, 43520, 43615, ENCODING_UTF8, "Cham, U+AA00-U+AA5F, (43520-43615");
        testBlock(midlet, 43616, 43647, ENCODING_UTF8, "Myanmar Extended-A, U+AA60-U+AA7F, (43616-43647");
        testBlock(midlet, 43648, 43743, ENCODING_UTF8, "Tai Viet, U+AA80-U+AADF, (43648-43743");
        testBlock(midlet, 43968, 44031, ENCODING_UTF8, "Meetei Mayek, U+ABC0-U+ABFF, (43968-44031");
        testBlock(midlet, 44032, 55203, ENCODING_UTF8, "Hangul Syllables, U+AC00-U+D7A3, (44032-55203");
        testBlock(midlet, 55216, 55295, ENCODING_UTF8, "Hangul Jamo Extended-B, U+D7B0-U+D7FF, (55216-55295");
        testBlock(midlet, 63744, 64255, ENCODING_UTF8, "CJK Compatibility Ideographs, U+F900-U+FAFF, (63744-64255");
        testBlock(midlet, 64256, 64335, ENCODING_UTF8, "Alphabetic Presentation Forms, U+FB00-U+FB4F, (64256-64335");
        testBlock(midlet, 64336, 65023, ENCODING_UTF8, "Arabic Presentation Forms-A, U+FB50-U+FDFF, (64336-65023");
        testBlock(midlet, 65024, 65039, ENCODING_UTF8, "Variation Selectors, U+FE00-U+FE0F, (65024-65039");
        testBlock(midlet, 65056, 65071, ENCODING_UTF8, "Combining Half Marks, U+FE20-U+FE2F, (65056-65071");
        testBlock(midlet, 65072, 65103, ENCODING_UTF8, "CJK Compatibility Forms, U+FE30-U+FE4F, (65072-65103");
        testBlock(midlet, 65104, 65135, ENCODING_UTF8, "Small Form Variants, U+FE50-U+FE6F, (65104-65135");
        testBlock(midlet, 65136, 65279, ENCODING_UTF8, "Arabic Presentation Forms-B, U+FE70-U+FEFF, (65136-65279");
        testBlock(midlet, 65280, 65519, ENCODING_UTF8, "Halfwidth and Fullwidth Forms, U+FF00-U+FFEF, (65280-65519");
        testBlock(midlet, 65520, 65535, ENCODING_UTF8, "Specials, U+FEFF, U+FFF0-U+FFFF, (65279, 65520-65535");

        Logger.log("+ Unicode blocks tested:  " + blocksTested + " (" + charsTested + " characters)");
        Logger.log("+ Unicode block failures: " + blocksFailed);

        /* These Unicode blocks are not supported in Java (as they require more than 2 bytes per character)
        testBlock(midlet, 0, 127, ENCODING_UTF8, "Linear B Syllabary, U+10000-U+1007F, (65536-65663");
        testBlock(midlet, 0, 127, ENCODING_UTF8, "Linear B Ideograms, U+10080-U+100FF, (65664-65791");
        testBlock(midlet, 0, 127, ENCODING_UTF8, "Aegean Numbers, U+10100-U+1013F, (65792-65855");
        testBlock(midlet, 0, 127, ENCODING_UTF8, "Ancient Greek Numbers, U+10140-U+1018F, (65856-65935");
        testBlock(midlet, 0, 127, ENCODING_UTF8, "Ancient Symbols, U+10190-U+101CF, (65936-65999");
        testBlock(midlet, 0, 127, ENCODING_UTF8, "Phaistos Disc, U+101D0-U+101FF, (66000-66047");
        testBlock(midlet, 0, 127, ENCODING_UTF8, "Lycian, U+10280-U+1029F, (66176-66207");
        testBlock(midlet, 0, 127, ENCODING_UTF8, "Carian, U+102A0-U+102DF, (66208-66271");
        testBlock(midlet, 0, 127, ENCODING_UTF8, "Old Italic, U+10300-U+1032F, (66304-66351");
        testBlock(midlet, 0, 127, ENCODING_UTF8, "Gothic, U+10330-U+1034F, (66352-66383");
        testBlock(midlet, 0, 127, ENCODING_UTF8, "Ugaritic, U+10380-U+1039F, (66432-66463");
        testBlock(midlet, 0, 127, ENCODING_UTF8, "Deseret, U+10400-U+1044F, (66560-66639");
        testBlock(midlet, 0, 127, ENCODING_UTF8, "Shavian, U+10450-U+1047F, (66640-66687");
        testBlock(midlet, 0, 127, ENCODING_UTF8, "Osmanya, U+10480-U+104AF, (66688-66735");
        testBlock(midlet, 0, 127, ENCODING_UTF8, "Cypriot Syllabary, U+10800-U+1083F, (67584-67647");
        testBlock(midlet, 0, 127, ENCODING_UTF8, "Imperial Aramaic, U+10840-U+1085F, (67648-67679");
        testBlock(midlet, 0, 127, ENCODING_UTF8, "Phoenician, U+10900-U+1091F, (67840-67871");
        testBlock(midlet, 0, 127, ENCODING_UTF8, "Lydian, U+10920-U+1093F, (67872-67903");
        testBlock(midlet, 0, 127, ENCODING_UTF8, "Kharoshthi, U+10A00-U+10A5F, (68096-68191");
        testBlock(midlet, 0, 127, ENCODING_UTF8, "Old South Arabian, U+10A60-U+10A7F, (68192-68223");
        testBlock(midlet, 0, 127, ENCODING_UTF8, "Avestan, U+10B00-U+10B3F, (68352-68415");
        testBlock(midlet, 0, 127, ENCODING_UTF8, "Inscriptional Parthian, U+10B40-U+10B5F, (68416-68447");
        testBlock(midlet, 0, 127, ENCODING_UTF8, "Inscriptional Pahlavi, U+10B60-U+10B7F, (68448-68479");
        testBlock(midlet, 0, 127, ENCODING_UTF8, "Old Turkic, U+10C00-U+10C4F, (68608-68687");
        testBlock(midlet, 0, 127, ENCODING_UTF8, "Rumi Numeral Symbols, U+10E60-U+10E7F, (69216-69247");
        testBlock(midlet, 0, 127, ENCODING_UTF8, "Kaithi, U+11080-U+110CF, (69760-69839");
        testBlock(midlet, 0, 127, ENCODING_UTF8, "Cuneiform, U+12000-U+123FF, (73728-74751");
        testBlock(midlet, 0, 127, ENCODING_UTF8, "Cuneiform Numbers and Punctuation, U+12400-U+1247F, (74752-74879");
        testBlock(midlet, 0, 127, ENCODING_UTF8, "Egyptian Hieroglyphs, U+13000-U+1342F, (77824-78895");
        testBlock(midlet, 0, 127, ENCODING_UTF8, "Byzantine Musical Symbols, U+1D000-U+1D0FF, (118784-119039");
        testBlock(midlet, 0, 127, ENCODING_UTF8, "Musical Symbols, U+1D100-U+1D1FF, (119040-119295");
        testBlock(midlet, 0, 127, ENCODING_UTF8, "Tai Xuan Jing Symbols, U+1D300-U+1D35F, (119552-119647");
        testBlock(midlet, 0, 127, ENCODING_UTF8, "Counting Rod Numerals, U+1D360-U+1D37F, (119648-119679");
        testBlock(midlet, 0, 127, ENCODING_UTF8, "Mathematical Alphanumeric Symbols, U+1D400-U+1D7FF, (119808-120831");
        testBlock(midlet, 0, 127, ENCODING_UTF8, "Mahjong Tiles, U+1F000-U+1F02F, (126976-127023");
        testBlock(midlet, 0, 127, ENCODING_UTF8, "Domino Tiles, U+1F030-U+1F09F, (127024-127135");
        testBlock(midlet, 0, 127, ENCODING_UTF8, "Enclosed Alphanumeric Supplement, U+1F100-U+1F1FF, (127232-127487");
        testBlock(midlet, 0, 127, ENCODING_UTF8, "Enclosed Ideographic Supplement, U+1F200-U+1F2FF, (127488-127743");
        testBlock(midlet, 0, 127, ENCODING_UTF8, "CJK Unified Ideographs Extension B, U+20000-U+2A6D6, (131072-173782");
        testBlock(midlet, 0, 127, ENCODING_UTF8, "CJK Unified Ideographs Extension C, U+2A700-U+2B73F, (173824-177983");
        testBlock(midlet, 0, 127, ENCODING_UTF8, "CJK Compatibility Ideographs Supplement, U+2F800-U+2FA1F, (194560-195103");
        testBlock(midlet, 0, 127, ENCODING_UTF8, "Tags, U+E0000-U+E007F, (917504-917631");
        testBlock(midlet, 0, 127, ENCODING_UTF8, "Variation Selectors Supplement, U+E0100-U+E01EF, (917760-917999");
        testBlock(midlet, 0, 127, ENCODING_UTF8, "Supplementary Private Use Area-A, U+F0000-U+FFFFD, (983040-1048573");
        testBlock(midlet, 0, 127, ENCODING_UTF8, "Supplementary Private Use Area-B, U+100000-U+10FFFD, (1048576-1114109");
        */
    }

    private static void testBlock(DiscoveryMIDlet midlet, int rangeStart, int rangeEnd, String encoding, String blockName)
    {
        //generate a string containing all the characters from the specified range
        StringBuffer string = new StringBuffer();
        for (int i = rangeStart; i <= rangeEnd; i++)
            string.append( (char)i );

        blocksTested++;
        charsTested += string.length();
        midlet.setTestStatus("Testing block " + blocksTested);
        if (! testEncoding(string.toString(), encoding, blockName))
            blocksFailed++;
    }
    
    private static boolean testEncoding(String testString, String encoding, String blockName)
    {
        //encode the string using the specified encoding
        byte[] encodedData = null;
        try
        {
            encodedData = testString.getBytes(encoding);
        }
        catch (Throwable e)
        {
            if (blockName != null)
                Logger.logIssue(Logger.SEVERITY_CRITICAL, "CharacterSets: failed to encode Unicode block ('" + blockName + "') with encoding ('" + encoding + "')", e);
            else
                Logger.logIssue(Logger.SEVERITY_CRITICAL, "CharacterSets: failed to encode string ('" + testString + "') with encoding ('" + encoding + "')", e);
            return false;
        }
        
        //decode the data (using the same encoding) and compare to the original string (should be exactly the same)
        String decodedString = null;
        try
        {
            decodedString = new String(encodedData, encoding);
            if (! decodedString.equals(testString))
            {
                if (blockName != null)
                    Logger.logIssue(Logger.SEVERITY_CRITICAL, "CharacterSets: character encoding ('" + encoding + "') isn't implemented correctly for Unicode block ('" + blockName + "')");
                else
                    Logger.logIssue(Logger.SEVERITY_CRITICAL, "CharacterSets: character encoding ('" + encoding + "') isn't implemented correctly (decoded string '" + decodedString + "' doesn't match original string '" + testString + "')");
            }

            return true;
        }
        catch (Throwable e)
        {
            if (blockName != null)
                Logger.logIssue(Logger.SEVERITY_CRITICAL, "CharacterSets: failed to decode Unicode data block ('" + blockName + "') with encoding ('" + encoding + "')");
            else
                Logger.logIssue(Logger.SEVERITY_CRITICAL, "CharacterSets: failed to decode data with encoding ('" + encoding + "')", e);
            return false;
        }
    }
}
