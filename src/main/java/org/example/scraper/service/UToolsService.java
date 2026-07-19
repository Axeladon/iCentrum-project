package org.example.scraper.service;

import org.example.scraper.exception.CrmIntegrationException;
import org.example.scraper.model.CrmDevice;
import org.example.scraper.model.CrmStatus;
import org.example.scraper.model.InfoFileModel;
import org.example.scraper.service.crm.CrmDataSender;
import org.example.scraper.service.threeutools.UToolsInfoFileService;

import java.net.http.HttpResponse;
import java.util.List;

public class UToolsService {

    private static final int UNCHECKED_PROBLEM_ID = 24;
    private static final int SKIP_RESERVATION_PROBLEM_ID = 26;
    private static final int NOT_CE_PROBLEM_ID = 28;

    private final UToolsInfoFileService infoFileServ;
        private final CrmDataSender crmDataSender;

    public UToolsService() {
        infoFileServ = new UToolsInfoFileService();
        crmDataSender = new CrmDataSender();
    }

    public CrmStatus sendDeviceToMainDataBase(CrmDevice device, List<Integer> selectedProblemIds) {

        List<Integer> problemIds = addProblemsBasedOnDeviceState(device, selectedProblemIds);

        try {
            HttpResponse<String> response = crmDataSender.sendDevice(device, problemIds);
            return crmDataSender.getCrmStatus(response);
        } catch (Exception e) {
            throw new CrmIntegrationException("Failed to send device to CRM", e);
        }
    }

    public List<InfoFileModel> loadAllInfoFiles() {
        return infoFileServ.loadAllInfoFiles();
    }

    public void deleteAllInfoFiles() {
        infoFileServ.deleteAllInfoFiles();
    }

    private List<Integer> addProblemsBasedOnDeviceState(CrmDevice device, List<Integer> selectedProblemIds) {
        if (selectedProblemIds == null) {
            selectedProblemIds = new java.util.ArrayList<>();
        }

        if (device.isUnchecked() && !selectedProblemIds.contains(UNCHECKED_PROBLEM_ID)) {
            selectedProblemIds.add(UNCHECKED_PROBLEM_ID);
        }

        if (!device.isCeCertificationMark()) {
            selectedProblemIds.add(SKIP_RESERVATION_PROBLEM_ID);
            selectedProblemIds.add(NOT_CE_PROBLEM_ID);
        }

        return selectedProblemIds;
    }
}
