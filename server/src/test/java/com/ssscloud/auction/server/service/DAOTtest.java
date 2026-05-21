package com.ssscloud.auction.server.service;

import static org.mockito.Mockito.lenient;

import java.util.List;

import com.ssscloud.auction.common.model.auction.Auction;
import com.ssscloud.auction.common.payload.response.DTO.BidderDisplayDTO;
import com.ssscloud.auction.server.dao.AuctionDAO;
import com.ssscloud.auction.server.dao.ItemDAO;

public class DAOTtest {
    public static void main(String[] args) throws Exception{
        AuctionDAO auctionDAO = new AuctionDAO();
        ItemDAO itemDAO = new ItemDAO();
        
        Auction auction = auctionDAO.findByAuctionId("ccd6339f-e08e-4f05-bcbd-a0b0983cd3d3");
        System.out.println(auction.getAuctionConfig().getName());
        System.out.println (auction.getBidCount());
    }
}