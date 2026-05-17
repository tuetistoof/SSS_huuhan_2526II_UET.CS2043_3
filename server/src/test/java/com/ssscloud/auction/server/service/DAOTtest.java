package com.ssscloud.auction.server.service;

import static org.mockito.Mockito.lenient;

import java.util.List;

import com.ssscloud.auction.common.dto.response.BidderDisplayDTO;
import com.ssscloud.auction.common.model.Auction;
import com.ssscloud.auction.server.dao.AuctionDAO;

public class DAOTtest {
    public static void main(String[] args) throws Exception{
        AuctionDAO auctionDAO = new AuctionDAO();
        Auction auction = auctionDAO.findByAuctionId("ccd6339f-e08e-4f05-bcbd-a0b0983cd3d3");
        System.out.println(auction.getAuctionConfig().getName());
        System.out.println (auction.getBidCount());
    }
}