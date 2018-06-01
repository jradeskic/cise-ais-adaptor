package eu.cise.adaptor.translate;

import eu.cise.adaptor.AdaptorConfig;
import eu.cise.datamodel.v1.entity.Entity;
import eu.cise.servicemodel.v1.authority.SeaBasinType;
import eu.cise.servicemodel.v1.message.*;
import eu.cise.servicemodel.v1.service.DataFreshnessType;
import eu.cise.servicemodel.v1.service.ServiceOperationType;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static eu.eucise.helpers.ParticipantBuilder.newParticipant;
import static eu.eucise.helpers.PushBuilder.newPush;
import static eu.eucise.helpers.ServiceBuilder.newService;

public class CiseModelToCiseMessage implements Translator<List<Entity>, Push> {
    private final AdaptorConfig config;

    public CiseModelToCiseMessage(AdaptorConfig config) {
        this.config = config;
    }

    @Override
    public Push translate(List<Entity> entity) {
        return newPush()
                .id(UUID.randomUUID().toString())
                .contextId(UUID.randomUUID().toString())
                .correlationId(UUID.randomUUID().toString())
                .creationDateTime(new Date())
                .sender(newService()
                        .id(config.getServiceId())
                        .dataFreshness(DataFreshnessType.fromValue(config.getDataFreshnessType()))
                        .seaBasin(SeaBasinType.fromValue(config.getSeaBasinType()))
                        .operation(ServiceOperationType.fromValue(config.getServiceOperation()))
                        .participant(newParticipant().endpointUrl(config.getEndpointUrl())))
                .recipient(newService()
                        .id(config.getRecipientServiceId())
                        .operation(ServiceOperationType.fromValue(config.getRecipientServiceOperation()))
                )
                .priority(PriorityType.fromValue(config.getMessagePriority()))
                .isRequiresAck(false)
                .informationSecurityLevel(InformationSecurityLevelType.fromValue(config.getSecurityLevel()))
                .informationSensitivity(InformationSensitivityType.fromValue(config.getSensitivity()))
                .isPersonalData(false)
                .purpose(PurposeType.fromValue(config.getPurpose()))
                .addEntities(entity)
                .build();
    }

}