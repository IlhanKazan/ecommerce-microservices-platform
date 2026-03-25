package com.ecommerce.stockservice.warehouse.mapper;

import com.ecommerce.stockservice.warehouse.controller.dto.response.WarehouseResponse;
import com.ecommerce.stockservice.warehouse.entity.Warehouse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface WarehouseMapper {

    WarehouseResponse toResponse(Warehouse warehouse);

    List<WarehouseResponse> toResponseList(List<Warehouse> warehouses);
}