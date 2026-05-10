package com.ssscloud.auction.common.dto.response;


public class ArtDTO extends ItemDTO {
    private static final long serialVersionUID = 1L;
    private boolean certificate;
    
    public void setCertificate(boolean certificate) {
        this.certificate = certificate;
    }
    public boolean getCertificate() {
        return certificate;
    }
}
