/*
 * Copyright CISE AIS Adaptor (c) 2018-2019, European Union
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package eu.cise.adaptor.translate;

import static java.lang.Boolean.FALSE;

import dk.tbsalling.aismessages.ais.messages.AISMessage;
import dk.tbsalling.aismessages.ais.messages.Metadata;
import dk.tbsalling.aismessages.ais.messages.types.ShipType;
import eu.cise.adaptor.AdaptorLogger;
import eu.cise.adaptor.AisMsg;
import eu.cise.adaptor.translate.utils.Eta;
import eu.cise.adaptor.translate.utils.EtaDate;
import eu.cise.adaptor.translate.utils.EtaTime;
import eu.cise.adaptor.translate.utils.NavigationStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * This classes normalize the AISMessage class read by the tbsalling's library into an internal
 * one.
 * <p>
 * The message is translated field by field in order to support many different AIS libraries.
 * <p>
 * The timestamp sometimes is not filled in the source AISMessage object and in this case the
 * timestamp field is filled with Instant.MIN value.
 */
public class AisMessageToAisMsg implements Translator<AISMessage, Optional<AisMsg>> {

    private final Eta eta;
    private final AdaptorLogger logger;

    public AisMessageToAisMsg(AdaptorLogger logger) {
        this(Clock.systemUTC(), logger);
    }

    public AisMessageToAisMsg(Clock clock, AdaptorLogger logger) {
        this.eta = new Eta(clock, new EtaDate(), new EtaTime());
        this.logger = logger;
    }

    @Override
    public Optional<AisMsg> translate(AISMessage m) {
        try {
            Integer type = m.getMessageType().getCode();
            AisMsg.Builder b = new AisMsg.Builder(type);

            if (messageTypeIsNot1235(type))
                return Optional.empty();

            // POSITION
            b.withUserId(m.getSourceMmsi().getMMSI());
            b.withLatitude((Float) m.dataFields().getOrDefault("latitude", 0F));
            b.withLongitude((Float) m.dataFields().getOrDefault("longitude", 0F));
            b.withPositionAccuracy(getPositionAccuracy(m.dataFields()));
            b.withCOG((Float) m.dataFields().getOrDefault("courseOverGround", 0F));
            b.withSOG((Float) m.dataFields().getOrDefault("speedOverGround", 0F));
            b.withTrueHeading((Integer) m.dataFields().getOrDefault("trueHeading", 0));
            b.withNavigationStatus(
                getNavigationStatus((String) m.dataFields().get("navigationStatus")));
            b.withTimestamp(oMeta(m).map(Metadata::getReceived).orElse(Instant.MIN));

            // VOYAGE
            b.withDestination((String) m.dataFields().getOrDefault("destination", ""));
            b.withEta(eta.computeETA((String) m.dataFields().get("eta")));
            b.withIMONumber((Integer) m.dataFields().getOrDefault("imo.IMO", 0));
            b.withCallSign((String) m.dataFields().getOrDefault("callsign", ""));
            b.withDraught((Float) m.dataFields().getOrDefault("draught", 0F));
            b.withDimensionC((Integer) m.dataFields().getOrDefault("toPort", 0));
            b.withDimensionD((Integer) m.dataFields().getOrDefault("toStarboard", 0));
            b.withDimensionA((Integer) m.dataFields().getOrDefault("toBow", 0));
            b.withDimensionB((Integer) m.dataFields().getOrDefault("toStern", 0));
            b.withShipType(translateShipType(m));
            b.withShipName((String) m.dataFields().getOrDefault("shipName", ""));

            return Optional.of(b.build());
        } catch (Exception e) {
            logger.logNMEADecodingError(m.toString(), e);
            return Optional.empty();
        }
    }

    private Integer translateShipType(AISMessage m) {
        String shipType = (String) m.dataFields().get("shipType");

        if (shipType == null)
            return 0;

        return ShipType.valueOf(shipType).getCode();
    }

    private NavigationStatus getNavigationStatus(String ns) {
        return ns == null ? null : NavigationStatus.valueOf(ns);
    }

    /**
     * @param m is a map containing the properties coming from the AIS unmarshalling
     * @return 1 if position accuracy lte 10m; 0 otherwise.
     */
    int getPositionAccuracy(Map<String, Object> m) {
        return (Boolean) m.getOrDefault("positionAccuracy", FALSE) ? 1 : 0;
    }

    private boolean messageTypeIsNot1235(Integer type) {
        return type != 1 && type != 2 && type != 3 && type != 5;
    }

    private Optional<Metadata> oMeta(AISMessage m) {
        return Optional.ofNullable(m.getMetadata());
    }

}
