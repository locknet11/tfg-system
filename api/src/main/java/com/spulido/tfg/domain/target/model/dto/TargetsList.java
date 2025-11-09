package com.spulido.tfg.domain.target.model.dto;

import java.util.List;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.spulido.tfg.domain.target.model.Target;

public class TargetsList extends PageImpl<Target> {

    public TargetsList(List<Target> content, Pageable pageable, long total) {
        super(content, pageable, total);
    }
}