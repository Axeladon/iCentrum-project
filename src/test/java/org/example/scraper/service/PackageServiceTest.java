package org.example.scraper.service;

import org.example.scraper.model.CrmDevice;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PackageServiceTest {

    @Mock
    private ThreeUToolsService threeUToolsService;

    @Test
    void shouldReturnValidImei() throws Exception {
        CrmDevice device = mock(CrmDevice.class);
        when(device.getImei()).thenReturn("351234567890123");
        when(threeUToolsService.createCrmDeviceFromDirectory()).thenReturn(device);

        PackageService service = new PackageService(threeUToolsService);

        String imei = service.getImei();

        assertNotNull(imei);
        assertTrue(imei.matches("^35\\d{13}$"));
    }

    @Test
    void shouldThrowWhenDeviceIsNull() throws Exception {
        when(threeUToolsService.createCrmDeviceFromDirectory()).thenReturn(null);

        PackageService service = new PackageService(threeUToolsService);

        assertThrows(IllegalStateException.class, service::getImei);
    }

    @Test
    void shouldThrowWhenImeiIsNull() throws Exception {
        CrmDevice device = mock(CrmDevice.class);
        when(device.getImei()).thenReturn(null);
        when(threeUToolsService.createCrmDeviceFromDirectory()).thenReturn(device);

        PackageService service = new PackageService(threeUToolsService);

        assertThrows(IllegalStateException.class, service::getImei);
    }
}
