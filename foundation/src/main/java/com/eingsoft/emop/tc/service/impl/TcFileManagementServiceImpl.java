package com.eingsoft.emop.tc.service.impl;

import com.eingsoft.emop.tc.BMIDE;
import com.eingsoft.emop.tc.annotation.ScopeDesc;
import com.eingsoft.emop.tc.annotation.ScopeDesc.Scope;
import com.eingsoft.emop.tc.service.TcContextHolder;
import com.eingsoft.emop.tc.service.TcFileManagementService;
import com.eingsoft.emop.tc.util.ICCTArgUtil;
import com.eingsoft.emop.tc.util.ProxyUtil;
import com.eingsoft.emop.tc.util.TCFileUtil;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.teamcenter.services.internal.strong.core._2011_06.ICT;
import com.teamcenter.services.loose.core._2006_03.FileManagement;
import com.teamcenter.services.loose.core._2006_03.FileManagement.DatasetFileInfo;
import com.teamcenter.services.loose.core._2006_03.FileManagement.GetDatasetWriteTicketsInputData;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.services.strong.core.FileManagementService;
import com.teamcenter.services.strong.core._2006_03.DataManagement.CreateDatasetsResponse;
import com.teamcenter.services.strong.core._2006_03.FileManagement.FileTicketsResponse;
import com.teamcenter.services.strong.core._2008_06.DataManagement.DatasetProperties2;
import com.teamcenter.soa.client.FileManagementUtility;
import com.teamcenter.soa.client.GetFileResponse;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.Property;
import com.teamcenter.soa.client.model.ServiceData;
import com.teamcenter.soa.client.model.strong.Dataset;
import com.teamcenter.soa.client.model.strong.ImanFile;
import com.teamcenter.soa.exceptions.NotLoadedException;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.eingsoft.emop.tc.util.ProxyUtil.spy;
import static com.eingsoft.emop.tc.util.ReflectionUtil.getTypeFromModelObject;

@Log4j2
@ScopeDesc(Scope.TcContextHolder)
public class TcFileManagementServiceImpl implements TcFileManagementService {

    @Getter
    private final TcContextHolder tcContextHolder;

    public TcFileManagementServiceImpl(TcContextHolder tcContextHolder) {
        this.tcContextHolder = tcContextHolder;
    }

    /**
     * 根据Dataset对象下载文件， 并且以文件命名的引用中的名称作为文件名（非FCC Cache中的名称）
     * 
     * @param dataset
     * @return
     */
    @Override
    public List<File> downLoadFiles(ModelObject dataset) {
        if (!(dataset instanceof Dataset)) {
            return Collections.emptyList();
        }
        List<File> resultFiles = Lists.newArrayList();
        try {
            ModelObject loadedDataset =
                tcContextHolder.getTcLoadService().loadProperties(dataset, Lists.newArrayList("ref_list", "ref_names"));
            Property refProperty = loadedDataset.getPropertyObject("ref_list");
            // 获取真实名称，否则通过fms获取的文件名称为存储在卷上面的名称
            List<String> tmpFileNames = refProperty.getDisplayableValues();
            List<String> fileNames = new ArrayList<String>();
            ModelObject[] refObjs = refProperty.getModelObjectArrayValue();
            if (refObjs == null || refObjs.length == 0) {
                log.warn("no ref file exists");
                return resultFiles;
            }
            List<ModelObject> imanFiles = new ArrayList<ModelObject>();
            int fileIndex = 0;
            for (ModelObject fileObj : refObjs) {
                if (fileObj instanceof ImanFile) {
                    imanFiles.add(fileObj);
                    fileNames.add(tmpFileNames.get(fileIndex));
                }
                fileIndex++;
            }
            FileManagementUtility fMSFileManagement = new FileManagementUtility(tcContextHolder.getConnection());
            GetFileResponse fileResp = fMSFileManagement.getFiles(imanFiles.toArray(new ModelObject[] {}));
            File[] files = fileResp.getFiles();
            if (files == null || files.length == 0) {
                log.warn("no file exists");
                return resultFiles;
            }
            File[] tmpFiles = new File[files.length];
            // 因下载源路径在fccCache上，并且获取真实名称后可能存在同名文件而导致获取到异常文件，此处删除fccCache旧文件，若删除异常（无权限），则放入临时目录
            for (int i = 0; i < files.length; i++) {
                tmpFiles[i] = changeFileName(files[i], fileNames.get(i));
            }
            resultFiles = Lists.newArrayList(tmpFiles);
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
        }
        return resultFiles;
    }

    /**
     * 大部分情况下，一个数据集文件，里面只有一个文件，所以提供此API， 并以数据集的文件名称作为文件名（有可能 数据集引用名是随机码+文件后缀）<br>
     * 注意CREO文件不推荐使用此API下载， CREO文件名可能附带 后缀 如 xxx.drg.2, 而且CREO文件一般包含多个文件
     */
    @Override
    public File downLoadFile(ModelObject dataset) {
        List<File> files = downLoadFiles(dataset);
        if (files == null || files.isEmpty()) {
            return null;
        }

        com.eingsoft.emop.tc.model.ModelObject object = ProxyUtil.spy(dataset, tcContextHolder);
        String name = object.getDisplayVal(BMIDE.PROP_OBJECT_NAME);
        File file = changeFileName(files.get(0), name);
        return file;
    }

    /**
     * 因下载源路径在fccCache上，并且获取真实名称后可能存在同名文件而导致获取到异常文件，此处删除fccCache旧文件，若删除异常（无权限），则放入临时目录
     * 
     * @param originFile
     * @param name xxxx xxx.txt
     * @return
     */
    private File changeFileName(File originFile, String name) {
        if (Strings.isNullOrEmpty(name)) {
            return originFile;
        }
        if (originFile.getName().equals(name) || Files.getNameWithoutExtension(originFile.getName()).equals(name)) {
            return originFile;
        }
        String fileExtension = Files.getFileExtension(originFile.getName());
        // 包含后缀名的文件名
        String newFileName = name.lastIndexOf("." + fileExtension) > 0 ? name : name + "." + fileExtension;
        // 因下载源路径在fccCache上，并且获取真实名称后可能存在同名文件而导致获取到异常文件，此处删除fccCache旧文件，若删除异常（无权限），则放入临时目录
        File newFile = new File(originFile.getParent() + File.separator + newFileName);
        // 尝试删除
        if (newFile.exists() && !newFile.delete()) {
            log.error("delete file: " + originFile.getParent() + File.separator + newFileName + "faild.");
        }
        // 改成真实名称 失败的话 则下载到临时目录
        File file = null;
        if (originFile.renameTo(newFile)) {
            file = newFile;
        } else {
            newFile = new File(System.getProperty("java.io.tmpdir") + newFileName);
            // 失败返回原始文件（未改成真实名称）
            file = originFile.renameTo(newFile) ? newFile : originFile;
        }
        return file;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<com.eingsoft.emop.tc.model.ModelObject, List<FileRetrival>>
        retrieveFiles(List<? extends ModelObject> datasets, @NonNull FilenameFilter filter) {
        Map<com.eingsoft.emop.tc.model.ModelObject, List<FileRetrival>> result = new HashMap<>();
        List<com.eingsoft.emop.tc.model.ModelObject> loadedDataset =
            tcContextHolder.getTcLoadService().loadProperties(datasets, Lists.newArrayList("ref_list"));
        for (com.eingsoft.emop.tc.model.ModelObject dataset : loadedDataset) {
            List<com.eingsoft.emop.tc.model.ModelObject> imanFiles =
                (List<com.eingsoft.emop.tc.model.ModelObject>)dataset.get("ref_list");
            imanFiles = imanFiles.stream().filter(f -> f instanceof ImanFile).filter(f -> filter.accept(f.get("original_file_name", String.class)))
                .collect(Collectors.toList());
            if (imanFiles.size() > 0) {
                result.put(dataset, imanFiles.stream().map(f -> new FileRetrival(f)).collect(Collectors.toList()));
                log.debug("try to retrive {} valid files for dataset {}", imanFiles.size(), dataset.getUid());
            } else {
                log.debug("no valid file for dataset " + dataset.getUid());
            }
        }
        List<FileRetrival> retrivals = result.values().stream().flatMap(List::stream).collect(Collectors.toList());
        for (int i = 0; i < retrivals.size(); i++) {
            retrivals.get(i).setIndex(i);
        }
        List<ModelObject> imanFiles = retrivals.stream().map(r -> r.imanFile).collect(Collectors.toList());
        FileManagementUtility fMSFileManagement = new FileManagementUtility(tcContextHolder.getConnection());
        GetFileResponse fileResp = fMSFileManagement.getFiles(imanFiles.toArray(new ModelObject[] {}));
        File[] files = fileResp.getFiles();
        if (files.length != imanFiles.size()) {
            throw new RuntimeException("some files are missing, trying to retrieve " + imanFiles.size()
                + " files, but only returned " + files.length);
        }
        result.values().stream().flatMap(List::stream).forEach(r -> r.pickupFile(files));
        return result;
    }

    @Override
    public Map<com.eingsoft.emop.tc.model.ModelObject, List<FileRetrival>>
        retrieveFiles(List<? extends ModelObject> datasets) {
        return retrieveFiles(datasets, FilenameFilter.ACCEPT_ALL);
    }

    @Override
    public List<String> retrieveFile(String datasetUid) {
        FileManagementUtility fMSFileManagement = new FileManagementUtility(this.tcContextHolder.getConnection());
        DataManagementService dmService = DataManagementService.getService(tcContextHolder.getConnection());
        FileManagementService tcFileMgmtService = FileManagementService.getService(tcContextHolder.getConnection());
        // load dataset instances
        ServiceData queryResp = dmService.loadObjects(new String[] {datasetUid});
        if (queryResp.sizeOfPlainObjects() == 0) {
            throw new RuntimeException("Object (" + datasetUid + ") doesn't exist.");
        }

        if (!(queryResp.getPlainObject(0) instanceof Dataset)) {
            throw new RuntimeException("Object (" + datasetUid + ") is not a DataSet instance, it is "
                + getTypeFromModelObject(queryResp.getPlainObject(0)).getClassName());
        }

        // get the ref_list
        ServiceData refQueryResult =
            dmService.getProperties(new ModelObject[] {queryResp.getPlainObject(0)}, new String[] {"ref_list"});
        if (refQueryResult.sizeOfPlainObjects() == 0) {
            throw new RuntimeException("No ref_list (" + datasetUid + ").");
        }

        List<ImanFile> imanFiles = new ArrayList<ImanFile>();
        try {
            for (ModelObject obj : ((Dataset)refQueryResult.getPlainObject(0)).get_ref_list()) {
                if (obj instanceof ImanFile) {
                    imanFiles.add((ImanFile)obj);
                }
            }
        } catch (NotLoadedException e1) {
            throw new RuntimeException(e1.fillInStackTrace());
        }

        log.info("There are " + imanFiles.size() + " ref files");
        if (imanFiles.size() == 0) {
            return Collections.emptyList();
        }

        FileTicketsResponse fileTicketResp =
            tcFileMgmtService.getFileReadTickets(imanFiles.toArray(new ImanFile[imanFiles.size()]));

        @SuppressWarnings("unchecked")
        Collection<String> ticekts = fileTicketResp.tickets.values();
        log.debug("The ticket info:" + String.join(",", ticekts));
        File[] files;
        try {
            files = fMSFileManagement.getFiles(ticekts.toArray(new String[ticekts.size()]));
            if (files != null) {
                log.debug("it exist {} file.", files.length);
                for (File file : files) {
                    log.debug("the file detail info: {}", file.getAbsolutePath());
                }
            }
        } catch (IOException e) {
            log.error("fail to retrieve file by ticekts " + ticekts, e);
            throw new RuntimeException(e.fillInStackTrace());
        }

        return Arrays.stream(files).map(File::getAbsolutePath).collect(Collectors.toList());
    }

    @Override
    public Dataset updateFile(@NonNull String datasetUid, @NonNull String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IllegalArgumentException(file.getAbsolutePath() + " doesn't exist.");
        }
        FileManagementUtility fMSFileManagement = new FileManagementUtility(this.tcContextHolder.getConnection());
        ModelObject obj = tcContextHolder.getTcLoadService().loadObject(datasetUid);
        if (obj == null) {
            throw new IllegalArgumentException("cannot find dataset by " + datasetUid);
        }
        if (!(obj instanceof Dataset)) {
            throw new IllegalArgumentException(
                "expect dataset type by " + datasetUid + ", but it is " + getTypeFromModelObject(obj).getName());
        }
        Dataset dataset = (Dataset)obj;
        String filename = file.getName();
        File uploadFile = file;
        try {
            ModelObject[] files = dataset.get_ref_list();
            if (files != null && files.length > 0) {
                log.info("dataset already contains files " + Arrays.stream(files)
                    .map(f -> spy(f, getTcContextHolder()).get(BMIDE.PROP_OBJECT_STRING)).collect(Collectors.toList())
                    + ", overwrite the first one.");
                filename = (String)spy(files[0], getTcContextHolder()).get(BMIDE.PROP_OBJECT_STRING);
                if (!file.getName().equals(filename)) {
                    /*
                     * keep the file name as the original first one, to make
                     * sure it is a replace action even though the filenames are
                     * different
                     */
                    File baseDir = new File(System.getProperty("java.io.tmpdir"));
                    uploadFile =
                        new File(baseDir + File.separator + System.currentTimeMillis() + File.separator + filename);
                    FileUtils.copyFile(file, uploadFile);
                    log.info("renamed upload file to " + uploadFile.getAbsolutePath());
                }
            }

            FileManagement.GetDatasetWriteTicketsInputData[] inputs =
                {getGetDatasetWriteTicketsInputData(dataset, uploadFile, filename)};

            ServiceData response = fMSFileManagement.putFiles(inputs);
            if (response.sizeOfPartialErrors() > 0) {
                getTcContextHolder().printAndLogMessageFromServiceData(response, true);
            }

            return (Dataset)dataset;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (uploadFile != file) {
                uploadFile.delete();
            }
        }
    }

    private GetDatasetWriteTicketsInputData getGetDatasetWriteTicketsInputData(ModelObject dataset, File file,
        String filename) {
        // Create a file to associate with dataset
        DatasetFileInfo fileInfo = new DatasetFileInfo();
        fileInfo.clientId = "emop_file_client";
        fileInfo.fileName = file.getAbsolutePath();
        fileInfo.namedReferencedName = TCFileUtil.getTCFileType(file.getName());
        fileInfo.isText = "Text".equalsIgnoreCase(TCFileUtil.getTCDatasetType(file.getName()));
        fileInfo.allowReplace = true;
        DatasetFileInfo[] fileInfos = {fileInfo};

        GetDatasetWriteTicketsInputData inputData = new GetDatasetWriteTicketsInputData();
        inputData.dataset = dataset;
        inputData.createNewVersion = true;
        inputData.datasetFileInfos = fileInfos;

        return inputData;
    }

    @Override
    public Dataset uploadFile(@NonNull String containerUid, @NonNull String filePath, @NonNull String datasetName,
        @NonNull String relType, boolean replaceExisting) {
        File file = new File(filePath);
        if (!file.exists() || file.isDirectory()) {
            throw new IllegalArgumentException("file " + file.getAbsolutePath() + " doesn't exist or it is a folder.");
        }

        ModelObject container = getTcContextHolder().getTcLoadService().loadObject(containerUid);

        // replace mode, try to replace, if existing dataset not found, add new one
        if (replaceExisting) {
            Map<com.eingsoft.emop.tc.model.ModelObject, List<com.eingsoft.emop.tc.model.ModelObject>> datesetMap =
                getTcContextHolder().getTcRelationshipService()
                    .findAllRelatedModelObjsByRelationAndType(Arrays.asList(container), relType, BMIDE.TYPE_DATASET);
            List<com.eingsoft.emop.tc.model.ModelObject> datesetList = datesetMap.get(container);
            if (datesetList != null && !datesetList.isEmpty()) {
                // preload object_name
                datesetList = getTcContextHolder().getTcLoadService().loadProperty(datesetList, BMIDE.PROP_OBJECT_NAME);
                Optional<com.eingsoft.emop.tc.model.ModelObject> datesetOpt =
                    datesetList.stream().filter(o -> datasetName.equals(o.get(BMIDE.PROP_OBJECT_NAME))).findFirst();
                if (datesetOpt.isPresent()) {
                    Dataset dateset = (Dataset)datesetOpt.get();
                    return updateFile(dateset.getUid(), filePath);
                }
            }
        }

        FileManagementUtility fMSFileManagement = new FileManagementUtility(getTcContextHolder().getConnection());
        DataManagementService dmService = getTcContextHolder().getDataManagementService();

        FileManagement.GetDatasetWriteTicketsInputData[] inputs =
            {getGetDatasetWriteTicketsInputData(dmService, container, file, datasetName, relType)};

        ServiceData response = fMSFileManagement.putFiles(inputs);

        if (response.sizeOfPartialErrors() > 0) {
            getTcContextHolder().printAndLogMessageFromServiceData(response, true);
        }

        return (Dataset)inputs[0].dataset;
    }

    private GetDatasetWriteTicketsInputData getGetDatasetWriteTicketsInputData(DataManagementService dmService,
        ModelObject container, File file, String datasetName, String relType) {
        // Create a Dataset
        DatasetProperties2 props = new DatasetProperties2();
        props.clientId = "datasetWriteTixTestClientId";
        props.type = TCFileUtil.getTCDatasetType(file.getName());
        if (container != null) {
            props.container = container;
        }
        props.name = datasetName;
        props.relationType = relType;
        props.description = "EMOP uploaded file";
        DatasetProperties2[] currProps = {props};

        CreateDatasetsResponse resp = dmService.createDatasets2(currProps);

        // Create a file to associate with dataset
        DatasetFileInfo fileInfo = new DatasetFileInfo();
        fileInfo.clientId = "emop_file_client";
        fileInfo.fileName = file.getAbsolutePath();
        fileInfo.namedReferencedName = TCFileUtil.getTCFileType(file.getName());
        fileInfo.isText = "Text".equalsIgnoreCase(TCFileUtil.getTCDatasetType(file.getName()));
        fileInfo.allowReplace = false;
        DatasetFileInfo[] fileInfos = {fileInfo};

        GetDatasetWriteTicketsInputData inputData = new GetDatasetWriteTicketsInputData();
        inputData.dataset = resp.output[0].dataset;
        inputData.createNewVersion = false;
        inputData.datasetFileInfos = fileInfos;

        return inputData;
    }

    @Override
    public void removeFileFromDataSet(ModelObject dataset, List<String> refNames, List<String> originalFilenames) {
        if (refNames.size() != originalFilenames.size()) {
          throw new IllegalArgumentException(
              "refNames " + refNames + " and originalFilenames " + originalFilenames + " size are not the same which will lead to tc crash");
        }
        String[] types = refNames.toArray(new String[refNames.size()]);
        String[] names = originalFilenames.toArray(new String[originalFilenames.size()]);
        try {
            ICT.Arg[] args = new ICT.Arg[] {ICCTArgUtil.createArg("Dataset"),
                ICCTArgUtil.createArg(dataset.getTypeObject().getTypeUid()), ICCTArgUtil.createArg(dataset.getUid()),
                ICCTArgUtil.createEntry(names), ICCTArgUtil.createEntry(types)};
            ServiceData data =
                getTcContextHolder().getICTService().invokeICTMethod("ICCTDataset", "removeFiles", args).serviceData;
            getTcContextHolder().printAndLogMessageFromServiceData(data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public static interface FilenameFilter {
        boolean accept(String filename);

        public static FilenameFilter ACCEPT_ALL = new FilenameFilter() {
            @Override
            public boolean accept(String filename) {
                return true;
            }
        };
    }

    @Data
    public static class FileRetrival {
        private final com.eingsoft.emop.tc.model.ModelObject imanFile;
        private int index;
        @Setter
        private File file;

        public FileRetrival(com.eingsoft.emop.tc.model.ModelObject imanFile) {
            this.imanFile = imanFile;
        }

        public void pickupFile(File[] files) {
            this.file = files[index];
        }
    }
}
