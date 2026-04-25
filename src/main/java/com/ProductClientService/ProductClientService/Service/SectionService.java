package com.ProductClientService.ProductClientService.Service;

import com.ProductClientService.ProductClientService.DTO.SectionItemRequest;
import com.ProductClientService.ProductClientService.DTO.SectionRequest;
import com.ProductClientService.ProductClientService.DTO.sections.SectionItemResponseDto;
import com.ProductClientService.ProductClientService.DTO.sections.SectionResponseDto;
import com.ProductClientService.ProductClientService.Model.Section;
import com.ProductClientService.ProductClientService.Model.Section.DataSource;
import com.ProductClientService.ProductClientService.Model.SectionItem;
import com.ProductClientService.ProductClientService.Model.SectionItemRepository;
import com.ProductClientService.ProductClientService.Repository.SectionRepository;
import com.ProductClientService.ProductClientService.Service.section.SectionHydrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SectionService {

    private final SectionRepository sectionRepository;
    private final SectionItemRepository sectionItemRepository;
    private final SectionHydrator hydrator;

    private static final int PAGE_TIMEOUT_MS = 400;

    public List<SectionResponseDto> getPage(String category, UUID userId) {
        OffsetDateTime now = OffsetDateTime.now();
        List<Section> sections = sectionRepository.findPageSections(category, now);

        return sections.stream()
                .map(s -> CompletableFuture.supplyAsync(() -> toDto(s, hydrator.hydrate(s, userId))))
                .map(f -> f.completeOnTimeout(null, PAGE_TIMEOUT_MS, TimeUnit.MILLISECONDS))
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

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
                .widgetKey(request.getWidgetKey() != null ? request.getWidgetKey() : "product_grid_v1")
                .dataSource(request.getDataSource() != null ? request.getDataSource() : DataSource.STATIC)
                .config(request.getConfig())
                .dataParams(request.getDataParams())
                .position(request.getPosition())
                .active(request.getActive() != null ? request.getActive() : false)
                .category(request.getCategory())
                .startsAt(request.getStartsAt())
                .endsAt(request.getEndsAt())
                .audience(request.getAudience())
                .version(1)
                .build();

        List<SectionItem> items = new ArrayList<>();
        if (request.getItems() != null) {
            for (SectionItemRequest i : request.getItems()) {
                SectionItem item = SectionItem.builder()
                        .section(section)
                        .itemType(i.getItemType())
                        .itemRefId(i.getItemRefId())
                        .position(i.getPosition())
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
        section.setWidgetKey(request.getWidgetKey() != null ? request.getWidgetKey() : section.getWidgetKey());
        section.setDataSource(request.getDataSource() != null ? request.getDataSource() : section.getDataSource());
        section.setConfig(request.getConfig());
        section.setDataParams(request.getDataParams());
        section.setPosition(request.getPosition());
        section.setActive(request.getActive() != null ? request.getActive() : section.isActive());
        section.setCategory(request.getCategory());
        section.setStartsAt(request.getStartsAt());
        section.setEndsAt(request.getEndsAt());
        section.setAudience(request.getAudience());
        section.setVersion(section.getVersion() + 1);

        section.getItems().clear();
        if (request.getItems() != null) {
            for (SectionItemRequest i : request.getItems()) {
                SectionItem item = SectionItem.builder()
                        .section(section)
                        .itemType(i.getItemType())
                        .itemRefId(i.getItemRefId())
                        .position(i.getPosition())
                        .metadata(i.getMetadata())
                        .build();
                section.getItems().add(item);
            }
        }

        return sectionRepository.save(section);
    }

    public void deleteSection(UUID sectionId) {
        sectionRepository.deleteById(sectionId);
    }

    private SectionResponseDto toDto(Section section, List<SectionItemResponseDto> items) {
        return SectionResponseDto.builder()
                .id(section.getId().toString())
                .title(section.getTitle())
                .widgetKey(section.getWidgetKey())
                .dataKind(section.getType().name())
                .position(section.getPosition())
                .config(section.getConfig())
                .items(items)
                .pagination(null)  // pagination is null for most sections; implement per-section logic later
                .build();
    }
}
