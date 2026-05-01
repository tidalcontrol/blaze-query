/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.azure.defender;

import com.blazebit.query.connector.base.DataFormats;
import com.blazebit.query.spi.DataFetchContext;
import com.blazebit.query.spi.DataFetcher;
import com.blazebit.query.spi.DataFetcherException;
import com.blazebit.query.spi.DataFormat;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Serializable;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * A data fetcher for Microsoft Defender Antivirus per-device health details, as
 * returned by the {@code /api/deviceavinfo} export API.
 *
 * @author Felix Hagemans
 * @since 2.4.4
 */
public class DefenderMachineAvHealthDataFetcher implements DataFetcher<DefenderMachineAvHealth>, Serializable {

	private static final String DEVICE_AV_INFO_URL = "https://api.securitycenter.microsoft.com/api/deviceavinfo";
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	public static final DefenderMachineAvHealthDataFetcher INSTANCE = new DefenderMachineAvHealthDataFetcher();

	private DefenderMachineAvHealthDataFetcher() {
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.componentMethodConvention( DefenderMachineAvHealth.class, DefenderConventionContext.INSTANCE );
	}

	@Override
	public List<DefenderMachineAvHealth> fetch(DataFetchContext context) {
		try {
			List<DefenderClientAccessor> accessors = DefenderConnectorConfig.DEFENDER_CLIENT.getAll( context );
			List<DefenderMachineAvHealth> list = new ArrayList<>();
			for ( DefenderClientAccessor accessor : accessors ) {
				String url = DEVICE_AV_INFO_URL;
				while ( url != null ) {
					HttpRequest request = HttpRequest.newBuilder()
							.uri( URI.create( url ) )
							.header( "Authorization", "Bearer " + accessor.getBearerToken() )
							.header( "Accept", "application/json" )
							.GET()
							.build();
					HttpResponse<String> response = accessor.getHttpClient()
							.send( request, HttpResponse.BodyHandlers.ofString() );
					if ( response.statusCode() != 200 ) {
						throw new DataFetcherException(
								"Defender deviceavinfo API returned HTTP " + response.statusCode() );
					}
					JsonNode root = OBJECT_MAPPER.readTree( response.body() );
					JsonNode values = root.get( "value" );
					if ( values != null && values.isArray() ) {
						for ( JsonNode node : values ) {
							list.add( DefenderMachineAvHealth.fromJson( accessor.getTenantId(), node ) );
						}
					}
					JsonNode nextLink = root.get( "@odata.nextLink" );
					url = nextLink != null && !nextLink.isNull() ? nextLink.asText() : null;
				}
			}
			return list;
		}
		catch (DataFetcherException e) {
			throw e;
		}
		catch (Exception e) {
			throw new DataFetcherException( "Could not fetch Defender device antivirus health", e );
		}
	}
}
