package com.spulido.tfg.domain.shared;

import org.springframework.data.domain.PageImpl;

public class ListMapper {
    public static <T> ResponseList<T> mapList(PageImpl<T> list) {
        ResponseList<T> response = new ResponseList<>();
        response.setContent(list.getContent());
        response.setTotalElements(list.getTotalElements());
        response.setTotalPages(list.getTotalPages());
        response.setPage(list.getNumber());
        response.setSize(list.getSize());
        return response;
    }
}
