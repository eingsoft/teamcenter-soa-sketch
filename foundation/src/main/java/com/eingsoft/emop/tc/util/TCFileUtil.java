package com.eingsoft.emop.tc.util;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;

import com.google.common.base.Strings;

public class TCFileUtil {

    /**
     * 文件的后缀名(小写)，对应TC中 数据集类型[0] 和引用类型[1]的 MAP <br>
     * 如：Excel 表格的后缀名为 xlsx，对应在TC中的数据集类型为 MSExcelX, 引用类型为excel
     */
    private static Map<String, String[]> extension2fileTypeAndRefMap = new HashMap<String, String[]>();

    static {
        extension2fileTypeAndRefMap.put("docx", new String[] {"MSWordX", "word"});
        extension2fileTypeAndRefMap.put("doc", new String[] {"MSWord", "word"});
        extension2fileTypeAndRefMap.put("xlsx", new String[] {"MSExcelX", "excel"});
        extension2fileTypeAndRefMap.put("xls", new String[] {"MSExcel", "excel"});
        extension2fileTypeAndRefMap.put("csv", new String[] {"MSExcelX", "excel"});
        extension2fileTypeAndRefMap.put("pptx", new String[] {"MSPowerPointX", "powerpoint"});
        extension2fileTypeAndRefMap.put("ppt", new String[] {"MSPowerPoint", "powerpoint"});

        extension2fileTypeAndRefMap.put("pcf", new String[] {"PCF", "PCF_file"});
        extension2fileTypeAndRefMap.put("pdf", new String[] {"PDF", "PDF_Reference"});

        extension2fileTypeAndRefMap.put("jpg", new String[] {"Image", "Image"});
        extension2fileTypeAndRefMap.put("jpeg", new String[] {"Image", "Image"});
        extension2fileTypeAndRefMap.put("png", new String[] {"Image", "Image"});
        extension2fileTypeAndRefMap.put("gif", new String[] {"Image", "Image"});
        extension2fileTypeAndRefMap.put("bmp", new String[] {"Image", "Image"});
        extension2fileTypeAndRefMap.put("psd", new String[] {"Image", "Image"});
        extension2fileTypeAndRefMap.put("psb", new String[] {"Image", "Image"});
        extension2fileTypeAndRefMap.put("pdd", new String[] {"Image", "Image"});
        extension2fileTypeAndRefMap.put("tiff", new String[] {"Image", "Image"});
        extension2fileTypeAndRefMap.put("eps", new String[] {"Image", "Image"});
        extension2fileTypeAndRefMap.put("mht", new String[] {"Image", "Image"});
        extension2fileTypeAndRefMap.put("mhtml", new String[] {"Image", "Image"});

        extension2fileTypeAndRefMap.put("msg", new String[] {"Outlook", "Outlook-Msg"});
        
        extension2fileTypeAndRefMap.put("mpp", new String[] {"MSProject", "Ms_Project_Doc"});
        extension2fileTypeAndRefMap.put("mpx", new String[] {"MSProject", "MsP_Exchange"});
        extension2fileTypeAndRefMap.put("mpd", new String[] {"MSProject", "MsP_Database"});
        extension2fileTypeAndRefMap.put("mpt", new String[] {"MSProject", "MsP_Template"});

        extension2fileTypeAndRefMap.put("zip", new String[] {"Zip", "ZIPFILE"});

        extension2fileTypeAndRefMap.put("txt", new String[] {"Text", "Text"});
        extension2fileTypeAndRefMap.put("log", new String[] {"Text", "Text"});
        extension2fileTypeAndRefMap.put("json", new String[] {"Text", "Text"});

        // Text文件在TC中适应所有文件类型，没有列举在上述常用文件中的类型，均可使用Text数据集表达类型
        // rar TC中 OOTB类型不支持
        extension2fileTypeAndRefMap.put("rar", new String[] {"Text", "Text"});
    }

    /**
     * 如果是文件名是以 .prt.*, .drw.*, .asm.* 结尾，则认为其为creo文件 <br>
     * 或者是文件名是以 .prt, .drw, .asm 结尾，则认为其为creo文件 <br>
     * 
     * @param fileName
     * @return
     */
    public static boolean isCreoFile(String fileName) {
        if (Strings.isNullOrEmpty(fileName)) {
            return false;
        }
        String creoReqx = ".*(prt|drw|asm|PRT|DRW|ASM)(\\.\\d*)?";
        return fileName.matches(creoReqx);
    }

    /**
     * BMIDE中定义的数据集类型
     * 
     * @param physicalFileName
     * @return
     */
    public static String getTCDatasetType(String physicalFileName) {
        String extension = FilenameUtils.getExtension(physicalFileName);
        if (extension2fileTypeAndRefMap.containsKey(extension.toLowerCase())) {
            return extension2fileTypeAndRefMap.get(extension.toLowerCase())[0];
        } else {// Text 可用于所有文件类型
            // return "Text";
        }

        throw new RuntimeException("unknown file type for Teamcenter: " + physicalFileName);
    }

    /**
     * BMIDE中对应的引用类型
     * 
     * @param physicalFileName
     * @return
     */
    public static String getTCFileType(String physicalFileName) {
        String extension = FilenameUtils.getExtension(physicalFileName);
        if (extension2fileTypeAndRefMap.containsKey(extension.toLowerCase())) {
            return extension2fileTypeAndRefMap.get(extension.toLowerCase())[1];
        } else {// Text 可用于所有文件类型
            // return "Text";
        }

        throw new RuntimeException("unknown file type for Teamcenter: " + physicalFileName);
    }
}
