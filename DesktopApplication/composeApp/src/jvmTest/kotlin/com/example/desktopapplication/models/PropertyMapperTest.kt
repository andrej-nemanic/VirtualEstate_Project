package com.example.desktopapplication.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PropertyMapperTest {

    @Test
    fun `toIngest preserves all fields and applies lng lat overrides`() {
        val property = Property(
            region = "Gorenjska", city = "Kranj", neighborhood = "Stražišče",
            offerType = "Prodaja", propertyType = "Hiša",
            size = 100.0, price = 300000.0,
            description = "test", propertyLink = "http://example.com", imageUrl = "http://img.example.com"
        )
        val ingest = PropertyMapper.toIngest(property, lng = 14.36, lat = 46.24)
        assertEquals("Gorenjska", ingest.region)
        assertEquals("Kranj", ingest.city)
        assertEquals("Stražišče", ingest.neighborhood)
        assertEquals("Prodaja", ingest.offerType)
        assertEquals("Hiša", ingest.propertyType)
        assertEquals(100.0, ingest.size)
        assertEquals(300000.0, ingest.price)
        assertEquals("test", ingest.description)
        assertEquals(14.36, ingest.lng)
        assertEquals(46.24, ingest.lat)
    }

    @Test
    fun `toIngest passes property's own lng lat when no override`() {
        val property = Property(
            city = "Kranj", propertyType = "Hiša", size = 50.0, price = 100000.0,
            lng = 14.0, lat = 46.0
        )
        val ingest = PropertyMapper.toIngest(property)
        assertEquals(14.0, ingest.lng)
        assertEquals(46.0, ingest.lat)
    }

    @Test
    fun `toIngest passes null lng lat when neither property nor override has them`() {
        val property = Property(city = "Kranj", propertyType = "Hiša", size = 50.0, price = 100000.0)
        val ingest = PropertyMapper.toIngest(property)
        assertNull(ingest.lng)
        assertNull(ingest.lat)
    }

    @Test
    fun `fromResponse handles null fields with defaults`() {
        val dto = PropertyResponseDto(_id = "abc123", city = "Kranj")
        val result = PropertyMapper.fromResponse(dto, localId = 1)
        assertEquals(1, result.id)
        assertEquals("abc123", result.apiId)
        assertEquals("", result.region)
        assertEquals("Kranj", result.city)
        assertEquals("", result.neighborhood)
        assertEquals("Prodaja", result.offerType)
        assertEquals("", result.propertyType)
        assertEquals(0.0, result.size)
        assertEquals(0.0, result.price)
        assertNull(result.description)
    }

    @Test
    fun `fromResponse extracts coordinates as lng lat when non-zero`() {
        val dto = PropertyResponseDto(
            _id = "x", city = "Kranj",
            coordinates = CoordinatesDto(type = "Point", coordinates = listOf(14.36, 46.24))
        )
        val result = PropertyMapper.fromResponse(dto, localId = 1)
        assertEquals(14.36, result.lng)
        assertEquals(46.24, result.lat)
    }

    @Test
    fun `fromResponse treats 0 0 coordinates as missing`() {
        val dto = PropertyResponseDto(
            _id = "x", city = "Kranj",
            coordinates = CoordinatesDto(type = "Point", coordinates = listOf(0.0, 0.0))
        )
        val result = PropertyMapper.fromResponse(dto, localId = 1)
        assertNull(result.lng)
        assertNull(result.lat)
    }

    @Test
    fun `toIngest preserves source field`() {
        val property = Property(
            city = "Maribor", propertyType = "Stanovanje",
            size = 60.0, price = 120000.0,
            source = "nepremicnina.si"
        )
        val ingest = PropertyMapper.toIngest(property)
        assertEquals("nepremicnina.si", ingest.source)
    }

    @Test
    fun `fromResponse maps source field`() {
        val dto = PropertyResponseDto(_id = "y", city = "Celje", source = "generator")
        val result = PropertyMapper.fromResponse(dto, localId = 1)
        assertEquals("generator", result.source)
    }
}
