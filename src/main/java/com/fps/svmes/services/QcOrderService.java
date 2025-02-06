package com.fps.svmes.services;

import com.fps.svmes.dto.dtos.dispatch.QcOrderDTO;
import com.fps.svmes.dto.requests.QcOrderRequest;

import java.util.List;


public interface QcOrderService {
    QcOrderDTO createQcOrder(QcOrderDTO request);
    QcOrderDTO updateQcOrder(Long id, QcOrderDTO request);
    QcOrderDTO getQcOrderById(Long id);
    List<QcOrderDTO> getAllQcOrders();
    void deleteQcOrder(Long id, Integer userId);
}
