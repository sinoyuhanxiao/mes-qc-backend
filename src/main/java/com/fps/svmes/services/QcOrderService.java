package com.fps.svmes.services;

import com.fps.svmes.dto.dtos.dispatch.QcOrderDTO;
import com.fps.svmes.dto.requests.QcOrderRequest;

import java.util.List;


public interface QcOrderService {
    QcOrderDTO createQcOrder(QcOrderRequest request, Integer userId);
    QcOrderDTO updateQcOrder(Long id, QcOrderRequest request, Integer userId);
    QcOrderDTO getQcOrderById(Long orderId);
    List<QcOrderDTO> getAllQcOrders();
    void deleteQcOrder(Long orderId, Integer userId);
    void pauseDispatch(Long orderId, Long dispatchId, Integer userId);
    void resumeDispatch(Long orderId, Long dispatchId, Integer userId);
}
