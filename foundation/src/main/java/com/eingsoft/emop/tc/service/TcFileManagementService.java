package com.eingsoft.emop.tc.service;

import java.io.File;
import java.util.List;
import java.util.Map;
import com.eingsoft.emop.tc.service.impl.TcFileManagementServiceImpl.FileRetrival;
import com.eingsoft.emop.tc.service.impl.TcFileManagementServiceImpl.FilenameFilter;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.strong.Dataset;

import lombok.NonNull;

public interface TcFileManagementService extends TcService {
    /**
     * 大部分情况下，一个数据集文件，里面只有一个文件，所以提供此API， 并以数据集的文件名称作为文件名（有可能 数据集引用名是随机码+文件后缀）<br>
     * 注意CREO文件不推荐使用此API下载， CREO文件名可能附带 后缀 如 xxx.drg.2, 而且CREO文件一般包含多个文件 <br>
     * 非数据集文件返回空文件
     * 
     * @param dataset
     * @return
     */
    File downLoadFile(ModelObject dataset);

    /**
     * 根据Dataset对象下载文件， 并且以文件命名的引用中的名称作为文件名（非FCC Cache中的名称）
     * 
     * @param dataset
     * @return
     */
    List<File> downLoadFiles(ModelObject dataset);

    /**
     * retrieve absolute path of files in dataset
     */
    List<String> retrieveFile(@NonNull String datasetId);

    /**
     * put file into dataset, if the dataset already contains any file (may be more than one), the first file will be
     * replaced with the given file, so pay attention that:
     * 
     * 1. this API won't take care the file format(such as word file, excel file, pdf file and so on), the caller should
     * make sure that different type files won't be updated wrongly.
     * 
     * 2. if there is already file in the dataset, the new uploading file will be renamed to the existing first file
     * name, to make sure the existing file could be overriten successfully.
     */
    Dataset updateFile(@NonNull String datasetUid, @NonNull String filePath);

    /**
     * upload file under an TC object
     * 
     * @param containerUid
     * @param filePath
     * @param datasetName
     * @param relType
     * @param replaceExisting
     * @return
     */
    Dataset uploadFile(@NonNull String containerUid, @NonNull String filePath, @NonNull String datasetName,
        @NonNull String relType, boolean replaceExisting);

    /**
     * remove file from dataset, if the filename is null, clear the dataset
     * 
     * @param dataset the dataset
     * @param refNames dataset ref_names, such as PrtFile
     * @param originalFilenames the file to be removed from dataset
     */
    void removeFileFromDataSet(@NonNull ModelObject dataset, List<String> refNames, List<String> originalFilenames);

    /**
     * download file by datasets in a batch
     * 
     * @return the key is dataset, and if there is no physical file, the file field in
     *         {@link com.eingsoft.emop.tc.service.impl.TcFileManagementServiceImpl.FileRetrival} will
     *         be null
     */
    Map<com.eingsoft.emop.tc.model.ModelObject, List<FileRetrival>> retrieveFiles(List<? extends ModelObject> datasets, FilenameFilter filter);

    Map<com.eingsoft.emop.tc.model.ModelObject, List<FileRetrival>> retrieveFiles(List<? extends ModelObject> datasets);

}
