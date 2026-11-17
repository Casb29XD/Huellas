package com.desarrolloMovielexample.huella.core.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Departamentos de Colombia con sus municipios principales. La ciudad se guarda como "Municipio, Departamento",
 * el mismo formato que usan el feed y el servidor (que guarda solo lo que va antes de la coma).
 */
// ponytail: capitales y municipios más poblados, no los 1.100+; cambiar por un JSON en assets si hace falta la lista completa.
object ColombiaLocations {
    val departments: Map<String, List<String>> = linkedMapOf(
        "Amazonas" to listOf("Leticia", "Puerto Nariño"),
        "Antioquia" to listOf("Medellín", "Bello", "Envigado", "Itagüí", "Sabaneta", "Rionegro", "Apartadó", "Turbo", "Caucasia", "La Ceja", "Marinilla", "Copacabana", "La Estrella", "Caldas"),
        "Arauca" to listOf("Arauca", "Saravena", "Tame"),
        "Atlántico" to listOf("Barranquilla", "Soledad", "Malambo", "Puerto Colombia", "Sabanalarga"),
        "Bolívar" to listOf("Cartagena", "Magangué", "Turbaco", "El Carmen de Bolívar", "Arjona"),
        "Boyacá" to listOf("Tunja", "Duitama", "Sogamoso", "Chiquinquirá", "Paipa", "Villa de Leyva"),
        "Caldas" to listOf("Manizales", "La Dorada", "Chinchiná", "Villamaría", "Riosucio"),
        "Caquetá" to listOf("Florencia", "San Vicente del Caguán"),
        "Casanare" to listOf("Yopal", "Aguazul", "Villanueva"),
        "Cauca" to listOf("Popayán", "Santander de Quilichao", "Puerto Tejada"),
        "Cesar" to listOf("Valledupar", "Aguachica", "Agustín Codazzi"),
        "Chocó" to listOf("Quibdó", "Istmina"),
        "Córdoba" to listOf("Montería", "Cereté", "Lorica", "Sahagún"),
        "Cundinamarca" to listOf("Bogotá", "Soacha", "Facatativá", "Zipaquirá", "Chía", "Fusagasugá", "Girardot", "Mosquera", "Madrid", "Funza", "Cajicá"),
        "Guainía" to listOf("Inírida"),
        "Guaviare" to listOf("San José del Guaviare"),
        "Huila" to listOf("Neiva", "Pitalito", "Garzón", "La Plata"),
        "La Guajira" to listOf("Riohacha", "Maicao", "Uribia"),
        "Magdalena" to listOf("Santa Marta", "Ciénaga", "Fundación"),
        "Meta" to listOf("Villavicencio", "Acacías", "Granada", "Puerto López"),
        "Nariño" to listOf("Pasto", "Tumaco", "Ipiales"),
        "Norte de Santander" to listOf("Cúcuta", "Ocaña", "Pamplona", "Villa del Rosario", "Los Patios"),
        "Putumayo" to listOf("Mocoa", "Puerto Asís"),
        "Quindío" to listOf("Armenia", "Calarcá", "La Tebaida", "Montenegro", "Quimbaya"),
        "Risaralda" to listOf("Pereira", "Dosquebradas", "Santa Rosa de Cabal", "La Virginia"),
        "San Andrés y Providencia" to listOf("San Andrés", "Providencia"),
        "Santander" to listOf("Bucaramanga", "Floridablanca", "Girón", "Piedecuesta", "Barrancabermeja", "San Gil"),
        "Sucre" to listOf("Sincelejo", "Corozal", "Sampués"),
        "Tolima" to listOf("Ibagué", "Espinal", "Melgar", "Honda", "Mariquita"),
        "Valle del Cauca" to listOf("Cali", "Palmira", "Buenaventura", "Tuluá", "Cartago", "Buga", "Jamundí", "Yumbo"),
        "Vaupés" to listOf("Mitú"),
        "Vichada" to listOf("Puerto Carreño"),
    )

    /** Todas las ciudades en formato "Municipio, Departamento". */
    val cities: List<String> = departments.flatMap { (dept, towns) -> towns.map { "$it, $dept" } }
}

/** Dos selectores encadenados: departamento y luego municipio. [value] es "Municipio, Departamento" o vacío. */
@Composable
fun ColombiaLocationFields(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    municipalityLabel: String,
    modifier: Modifier = Modifier,
    error: String? = null,
) {
    // Guarda el departamento elegido aunque todavía no haya municipio (value vacío).
    var department by remember { mutableStateOf(value.substringAfter(", ", "")) }
    if (value.isNotEmpty()) department = value.substringAfter(", ", department)
    val municipality = value.substringBefore(",").takeIf { value.isNotEmpty() }.orEmpty()

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HuellaDropdownField(
            value = department,
            options = ColombiaLocations.departments.keys.toList(),
            onSelect = {
                if (it != department) onValueChange("")
                department = it
            },
            label = label,
            modifier = Modifier.fillMaxWidth(),
        )
        HuellaDropdownField(
            value = municipality,
            options = ColombiaLocations.departments[department].orEmpty(),
            onSelect = { onValueChange("$it, $department") },
            label = municipalityLabel,
            error = error,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
