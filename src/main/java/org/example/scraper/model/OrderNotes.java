package org.example.scraper.model;

import lombok.Setter;

@Setter
public class OrderNotes {
    private String clientNote;
    private String adminPrivateNote;
    private String adminPublicNote;

    public String getClientNoteOrDefault() {
        return clientNote == null || clientNote.isBlank()
                ? ""
                : clientNote;
    }

    public String getAdminPrivateNoteOrDefault() {
        return adminPrivateNote == null || adminPrivateNote.isBlank()
                ? ""
                : adminPrivateNote;
    }

    public String getAdminPublicNoteOrDefault() {
        return adminPublicNote == null || adminPublicNote.isBlank()
                ? ""
                : adminPublicNote;
    }
}
