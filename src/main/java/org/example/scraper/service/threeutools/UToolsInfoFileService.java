package org.example.scraper.service.threeutools;

import org.example.scraper.exception.SelectedDirectoryException;
import org.example.scraper.model.InfoFileModel;
import org.example.scraper.service.settings.SettingsService;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class UToolsInfoFileService {

    private static final String SELECTED_DIR_ERROR = "3uTools folder not selected";
    private static final String KEY_FOLDER = "threeutools_folder";

    private final UToolsInfoFileStorage infoFileStorage;

    public UToolsInfoFileService() {
        Path directory = getSelectedDirectoryOrThrow();
        infoFileStorage = new UToolsInfoFileStorage(directory);
    }

    public Path getSelectedDirectoryOrThrow() {
        String folder = SettingsService.loadString(KEY_FOLDER, "");

        try {
            Path p = Path.of(folder);

            if (!Files.isDirectory(p)) {
                throw new SelectedDirectoryException(SELECTED_DIR_ERROR);
            }
            return p;

        } catch (InvalidPathException e) {
            throw new SelectedDirectoryException(SELECTED_DIR_ERROR, e);
        }
    }

    public InfoFileModel buildInfoFile(Path path) {

        List<String> lines = infoFileStorage.readInfoFile(path);
        UToolsInfoFileParser parser = new UToolsInfoFileParser(lines);

        return InfoFileModel.builder()
                .pathToFile(path)
                .imei(parser.getValueByKey("InternationalMobileEquipmentIdentity"))
                .imei2(parser.getValueByKey("InternationalMobileEquipmentIdentity2"))
                .serialNumber(parser.getValueByKey("SerialNumber"))
                .productType(parser.getValueByKey("ProductType"))
                .regionInfo(parser.getValueByKey("RegionInfo"))
                .deviceEnclosureColor(parser.getValueByKey("DeviceEnclosureColor"))
                .uniqueChipId(parser.getValueByKey("UniqueChipID"))
                .uniqueDeviceId(parser.getValueByKey("UniqueDeviceID"))
                .modelNumber(parser.getValueByKey("ModelNumber"))
                .build();
    }

    public List<InfoFileModel> loadAllInfoFiles() {
        List<Path> paths = getAllInfoFilesPaths();
        List<InfoFileModel> infoFiles = new ArrayList<>();

        for (Path path : paths) {
            infoFiles.add(buildInfoFile(path));
        }

        return getNonEmptyInfoFiles(infoFiles);
    }

    private List<InfoFileModel> getNonEmptyInfoFiles(List<InfoFileModel> infoFileModelList) {
        List<InfoFileModel> result = new ArrayList<>();

        for (InfoFileModel model : infoFileModelList) {
            Path path = model.getPathToFile();

            String fileName = path.getFileName().toString();

            if (fileName.matches("^[A-Z0-9]+_info\\.txt$")) {
                result.add(model);
            }
        }
        return result;
    }

    public void deleteInfoFile(Path path) {
        infoFileStorage.deleteInfoFile(path);
    }

    public void deleteAllInfoFiles() {
        List<Path> paths = getAllInfoFilesPaths();
        for (Path path : paths) {
            deleteInfoFile(path);
        }
    }

    public int countAllInfoFiles() {
        return getAllInfoFilesPaths().size();
    }

    private List<Path> getAllInfoFilesPaths() {
        return infoFileStorage.getAllInfoFilesPaths();
    }
}
