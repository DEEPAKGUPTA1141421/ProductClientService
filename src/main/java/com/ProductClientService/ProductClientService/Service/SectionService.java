package com.ProductClientService.ProductClientService.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.ProductClientService.ProductClientService.DTO.SectionItemRequest;
import com.ProductClientService.ProductClientService.DTO.SectionRequest;
import com.ProductClientService.ProductClientService.Model.Section;
import com.ProductClientService.ProductClientService.Model.SectionItem;
import com.ProductClientService.ProductClientService.Model.SectionItemRepository;
import com.ProductClientService.ProductClientService.Repository.SectionRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SectionService {

    private final SectionRepository sectionRepository;
    private final SectionItemRepository sectionItemRepository;

    public List<Section> getSectionsByCategory(String category) {
        return sectionRepository.findByCategoryAndActiveTrueOrderByPositionAsc(category);
    }

    public List<SectionItem> getItemsForSection(UUID sectionId) {
        return sectionItemRepository.findBySectionId(sectionId);
    }

    public Section createSection(SectionRequest request) {
        Section section = Section.builder()
                .title(request.getTitle())
                .type(request.getType())
                .config(request.getConfig())
                .position(request.getPosition())
                .active(request.getActive())
                .category(request.getCategory())
                .build();

        List<SectionItem> items = new ArrayList<>();
        if (request.getItems() != null) {
            for (SectionItemRequest i : request.getItems()) {
                SectionItem item = SectionItem.builder()
                        .section(section)
                        .itemType(i.getItemType())
                        .itemRefId(i.getItemRefId())
                        .metadata(i.getMetadata())
                        .build();
                items.add(item);
            }
        }
        section.setItems(items);

        return sectionRepository.save(section);
    }

    public Section updateSection(UUID sectionId, SectionRequest request) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new RuntimeException("Section not found"));

        section.setTitle(request.getTitle());
        section.setType(request.getType());
        section.setConfig(request.getConfig());
        section.setPosition(request.getPosition());
        section.setActive(request.getActive());
        section.setCategory(request.getCategory());

        // Clear old items and replace with new ones
        section.getItems().clear();
        if (request.getItems() != null) {
            for (SectionItemRequest i : request.getItems()) {
                SectionItem item = SectionItem.builder()
                        .section(section)
                        .itemType(i.getItemType())
                        .itemRefId(i.getItemRefId())
                        .metadata(i.getMetadata())
                        .build();
                section.getItems().add(item);
            }
        }

        return sectionRepository.save(section);
    }
}
