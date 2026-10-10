package dgtic.core.service;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import dgtic.core.model.dto.reporte.ReporteConjuntoDTO;
import dgtic.core.model.dto.reporte.ReporteContenedorDTO;
import dgtic.core.model.dto.reporte.ReporteHojaDTO;
import dgtic.core.model.dto.reporte.ReporteMovimientoDTO;
import dgtic.core.model.dto.reporte.ReporteOperacionDetalleDTO;
import dgtic.core.model.dto.reporte.ReporteOperacionHojaDTO;

/**
 * Infraestructura común para la generación de reportes XLSX.
 *
 * Los datos, filtros y reglas de acceso permanecen en ReporteService. Esta
 * clase sólo transforma DTOs autorizados y metadata en celdas Excel.
 */
@Service
public class ReporteExcelService {
    // Fila de encabezado de tabla, después de metadata y título
    private static final int FILA_ENCABEZADO = 7;
    // Fila de título, metadata y filtros
    public XSSFWorkbook crearWorkbook() {
        return new XSSFWorkbook();
    }
    // Generación de reportes
    public byte[] generarHojas(List<ReporteHojaDTO> reportes, Metadata metadata) {
        // Genera un archivo Excel con una hoja de "Hojas de Producción" y los datos de reportes
        return generarArchivo((workbook, estilos) -> {
            // Crear hoja de "Hojas de Producción"
            Sheet sheet = workbook.createSheet("Hojas de Producción");
            // Escribir metadata en las primeras filas
            escribirMetadata(sheet, metadata, estilos);
            // Escribir encabezados de tabla en la fila 7
            Row encabezado = sheet.createRow(FILA_ENCABEZADO);
            // Escribir encabezados de tabla con estilo
            escribirEncabezados(encabezado, new String[]{
                    "Hoja", "Proyecto", "Cliente", "Estado", "Fecha salida",
                    "Regreso estimado", "Solicitado", "Surtido", "Devuelto",
                    "% Surtido", "% Recepción"
            }, estilos.encabezadoTabla());
            // Escribir datos de reportes a partir de la fila 8
            int fila = FILA_ENCABEZADO + 1;
            // Iterar sobre los reportes y escribir cada uno en una fila
            for (ReporteHojaDTO reporte : reportes) {
                Row row = sheet.createRow(fila++);
                escribirEntero(row.createCell(0), reporte.getIdHoja(), estilos.entero());
                escribirTexto(row.createCell(1), reporte.getNombreProyecto(), estilos.texto());
                escribirTexto(row.createCell(2), reporte.getCliente(), estilos.texto());
                escribirTexto(row.createCell(3), reporte.getEstado(), estilos.texto());
                escribirFechaHora(row.createCell(4), reporte.getFechaSalida(), estilos.fechaHora());
                escribirFechaHora(row.createCell(5), reporte.getFechaEstimadaRegreso(), estilos.fechaHora());
                escribirEntero(row.createCell(6), reporte.getTotalSolicitado(), estilos.entero());
                escribirEntero(row.createCell(7), reporte.getTotalSurtido(), estilos.entero());
                escribirEntero(row.createCell(8), reporte.getTotalDevuelto(), estilos.entero());
                escribirPorcentaje(row.createCell(9), reporte.getPorcentajeSurtido(), estilos.porcentaje());
                escribirPorcentaje(row.createCell(10), reporte.getPorcentajeRecepcion(), estilos.porcentaje());
            }
            // Configurar tabla y ajustar columnas
            configurarTabla(sheet, FILA_ENCABEZADO, fila - 1, 10);
            // Ajustar el ancho de las columnas automáticamente
            ajustarColumnas(sheet, 11);
        });
    }
    // Generación de reportes de movimientos
    public byte[] generarMovimientos(List<ReporteMovimientoDTO> reportes, Metadata metadata) {
        // Genera un archivo Excel con una hoja de "Movimientos" y los datos de reportes
        return generarArchivo((workbook, estilos) -> {
            // Crear hoja de "Movimientos"
            Sheet sheet = workbook.createSheet("Movimientos");
            // Escribir metadata en las primeras filas
            escribirMetadata(sheet, metadata, estilos);
            // Escribir encabezados de tabla en la fila 7
            Row encabezado = sheet.createRow(FILA_ENCABEZADO);
            // Escribir encabezados de tabla con estilo
            escribirEncabezados(encabezado, new String[]{
                    "Fecha", "Tipo", "Hoja", "Proyecto", "Conjunto", "Producto",
                    "Contenedor", "Usuario", "Observaciones"
            }, estilos.encabezadoTabla());
            // Escribir datos de reportes a partir de la fila 8
            int fila = FILA_ENCABEZADO + 1;
            // Iterar sobre los reportes y escribir cada uno en una fila
            for (ReporteMovimientoDTO reporte : reportes) {
                // Crear una nueva fila en la hoja
                Row row = sheet.createRow(fila++);
                // Escribir los valores de cada columna en la fila con el estilo correspondiente
                escribirFechaHora(row.createCell(0), reporte.getFecha(), estilos.fechaHora());
                escribirTexto(row.createCell(1), reporte.getTipoMovimiento(), estilos.texto());
                escribirEntero(row.createCell(2), reporte.getIdHoja(), estilos.entero());
                escribirTexto(row.createCell(3), reporte.getProyecto(), estilos.texto());
                escribirTexto(row.createCell(4), reporte.getCodigoConjunto(), estilos.texto());
                escribirTexto(row.createCell(5), reporte.getProducto(), estilos.texto());
                escribirTexto(row.createCell(6), reporte.getCodigoContenedor() == null
                        ? "Sin contenedor" : reporte.getCodigoContenedor(), estilos.texto());
                escribirTexto(row.createCell(7), reporte.getUsuario(), estilos.texto());
                escribirTexto(row.createCell(8), reporte.getObservaciones(), estilos.observaciones());
            }
            // Configurar tabla y ajustar columnas
            configurarTabla(sheet, FILA_ENCABEZADO, fila - 1, 8);
            // Ajustar el ancho de las columnas automáticamente
            ajustarColumnas(sheet, 9);
        });
    }
    // Generación de reportes de contenedores
    public byte[] generarContenedores(List<ReporteContenedorDTO> reportes, Metadata metadata) {
        // Genera un archivo Excel con una hoja de "Utilización de Contenedores" y los datos de reportes
        return generarArchivo((workbook, estilos) -> {
            // Crear hoja de "Utilización de Contenedores"
            Sheet sheet = workbook.createSheet("Utilización de Contenedores");
            // Escribir metadata en las primeras filas
            escribirMetadata(sheet, metadata, estilos);
            // Escribir encabezados de tabla en la fila 7
            Row encabezado = sheet.createRow(FILA_ENCABEZADO);
            // Escribir encabezados de tabla con estilo
            escribirEncabezados(encabezado, new String[]{
                    "Contenedor", "Hoja", "Proyecto", "Capacidad", "Salidas",
                    "% Utilización", "Fecha asignación", "Usuario asignación",
                    "Fecha cierre", "Usuario cierre", "Fecha liberación", "Usuario liberación"
            }, estilos.encabezadoTabla());
            // Escribir datos de reportes a partir de la fila 8
            int fila = FILA_ENCABEZADO + 1;
            // Iterar sobre los reportes y escribir cada uno en una fila
            for (ReporteContenedorDTO reporte : reportes) {
                // Crear una nueva fila en la hoja
                Row row = sheet.createRow(fila++);
                // Escribir los valores de cada columna en la fila con el estilo correspondiente
                escribirTexto(row.createCell(0), reporte.getCodigoContenedor(), estilos.texto());
                escribirEntero(row.createCell(1), reporte.getIdHoja(), estilos.entero());
                escribirTexto(row.createCell(2), reporte.getProyecto(), estilos.texto());
                escribirEntero(row.createCell(3), reporte.getCapacidad(), estilos.entero());
                escribirEntero(row.createCell(4), reporte.getCantidadSalidas(), estilos.entero());
                escribirPorcentaje(row.createCell(5), reporte.getPorcentajeUtilizacion(), estilos.porcentaje());
                escribirFechaHora(row.createCell(6), reporte.getFechaAsignacion(), estilos.fechaHora());
                escribirTexto(row.createCell(7), reporte.getUsuarioAsignacion(), estilos.texto());
                escribirFechaHora(row.createCell(8), reporte.getFechaCierreCarga(), estilos.fechaHora());
                escribirTexto(row.createCell(9), reporte.getUsuarioCierre(), estilos.texto());
                escribirFechaHora(row.createCell(10), reporte.getFechaLiberacion(), estilos.fechaHora());
                escribirTexto(row.createCell(11), reporte.getUsuarioLiberacion(), estilos.texto());
            }
            // Configurar tabla y ajustar columnas
            configurarTabla(sheet, FILA_ENCABEZADO, fila - 1, 11);
            // Ajustar el ancho de las columnas automáticamente
            ajustarColumnas(sheet, 12);
        });
    }
    // Generación de reportes de inventario
    public byte[] generarInventario(List<ReporteConjuntoDTO> reportes, Metadata metadata) {
        // Genera un archivo Excel con una hoja de "Inventario de Conjuntos" y los datos de reportes
        return generarArchivo((workbook, estilos) -> {
            // Crear hoja de "Inventario de Conjuntos"
            Sheet sheet = workbook.createSheet("Inventario de Conjuntos");
            // Escribir metadata en las primeras filas
            escribirMetadata(sheet, metadata, estilos);
            // Escribir encabezados de tabla en la fila 7
            Row encabezado = sheet.createRow(FILA_ENCABEZADO);
            // Escribir encabezados de tabla con estilo
            escribirEncabezados(encabezado, new String[]{
                    "Conjunto", "Producto", "Estado", "Fecha alta", "Observaciones"
            }, estilos.encabezadoTabla());
            // Escribir datos de reportes a partir de la fila 8
            int fila = FILA_ENCABEZADO + 1;
            // Iterar sobre los reportes y escribir cada uno en una fila
            for (ReporteConjuntoDTO reporte : reportes) {
                // Crear una nueva fila en la hoja
                Row row = sheet.createRow(fila++);
                // Escribir los valores de cada columna en la fila con el estilo correspondiente
                escribirTexto(row.createCell(0), reporte.getCodigoConjunto(), estilos.texto());
                escribirTexto(row.createCell(1), reporte.getProducto(), estilos.texto());
                escribirTexto(row.createCell(2), reporte.getEstado(), estilos.texto());
                escribirFechaHora(row.createCell(3), reporte.getFechaAlta(), estilos.fechaHora());
                escribirTexto(row.createCell(4), reporte.getObservaciones(), estilos.observaciones());
            }
            // Configurar tabla y ajustar columnas
            configurarTabla(sheet, FILA_ENCABEZADO, fila - 1, 4);
            // Ajustar el ancho de las columnas automáticamente
            ajustarColumnas(sheet, 5);
        });
    }
    // Generación de reportes de operaciones
    public byte[] generarSurtido(ReporteOperacionHojaDTO reporte, Metadata metadata) {
        // Genera un archivo Excel con una hoja de "Surtido" y los datos de reporte
        return generarOperacion(reporte, metadata, true);
    }
    // Generación de reportes de operaciones
    public byte[] generarRecepcion(ReporteOperacionHojaDTO reporte, Metadata metadata) {
        // Genera un archivo Excel con una hoja de "Recepción" y los datos de reporte
        return generarOperacion(reporte, metadata, false);
    }
    // Creación de estilos de celdas
    public Estilos crearEstilos(Workbook workbook) {
        // Crear estilos de celdas para títulos, encabezados, texto, enteros, porcentajes y fechas
        // Crear fuente para título
        Font tituloFont = workbook.createFont();
        // Poner en negrita y tamaño 14 para el título
        tituloFont.setBold(true);
        // Poner tamaño 14 para el título
        tituloFont.setFontHeightInPoints((short) 14);
        // Crear fuente para encabezado de tabla
        Font encabezadoFont = workbook.createFont();
        // Poner en negrita para el encabezado de tabla
        encabezadoFont.setBold(true);
        // Crear estilo para título
        CellStyle titulo = workbook.createCellStyle();
        // Poner en negrita y tamaño 14 para el título
        titulo.setFont(tituloFont);
        // Alineación vertical centrada para el título
        titulo.setVerticalAlignment(VerticalAlignment.CENTER);
        // Crear estilo para metadata
        CellStyle metadata = workbook.createCellStyle();
        // Alineación vertical centrada para metadata
        metadata.setVerticalAlignment(VerticalAlignment.CENTER);
        // Crear estilo para encabezado de tabla
        CellStyle encabezadoTabla = workbook.createCellStyle();
        // Poner en negrita para el encabezado de tabla
        encabezadoTabla.setFont(encabezadoFont);
        // Fondo gris claro para encabezado de tabla
        encabezadoTabla.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        // Patrón sólido para el fondo
        encabezadoTabla.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        // Bordes finos para el encabezado de tabla
        encabezadoTabla.setBorderBottom(BorderStyle.THIN);
        // Bordes finos para el encabezado de tabla
        encabezadoTabla.setAlignment(HorizontalAlignment.CENTER);
        // Alineación vertical centrada para el encabezado de tabla
        encabezadoTabla.setVerticalAlignment(VerticalAlignment.CENTER);
        // Permitir ajuste de texto en el encabezado de tabla
        encabezadoTabla.setWrapText(true);
        // Crear estilo para texto
        CellStyle texto = workbook.createCellStyle();
        texto.setVerticalAlignment(VerticalAlignment.TOP);
        // Crear estilo para enteros
        CellStyle entero = workbook.createCellStyle();
        entero.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));
        // Crear estilo para porcentajes
        CellStyle porcentaje = workbook.createCellStyle();
        porcentaje.setDataFormat(workbook.createDataFormat().getFormat("0.00%"));
        // Crear estilo para fechas
        CellStyle fecha = workbook.createCellStyle();
        fecha.setDataFormat(workbook.createDataFormat().getFormat("dd/mm/yyyy"));
        // Crear estilo para fechas y horas
        CellStyle fechaHora = workbook.createCellStyle();
        fechaHora.setDataFormat(workbook.createDataFormat().getFormat("dd/mm/yyyy hh:mm"));
        // Crear estilo para observaciones
        CellStyle observaciones = workbook.createCellStyle();
        observaciones.setVerticalAlignment(VerticalAlignment.TOP);
        observaciones.setWrapText(true);
        // Devolver los estilos creados en un objeto Estilos
        return new Estilos(titulo, metadata, encabezadoTabla, texto, entero, porcentaje,
                fecha, fechaHora, observaciones);
    }
    // Escribir metadata en la hoja
    public void escribirMetadata(Sheet sheet, Metadata metadata, Estilos estilos) {
        // Escribir título, bodega, usuario y fecha de generación en las primeras filas
        escribirTexto(sheet.createRow(0).createCell(0), "SGREA", estilos.titulo());
        escribirTexto(sheet.createRow(1).createCell(0), metadata.titulo(), estilos.metadata());
        escribirTexto(sheet.createRow(2).createCell(0), "Bodega: " + metadata.bodega(), estilos.metadata());
        escribirTexto(sheet.createRow(3).createCell(0), "Usuario: " + metadata.usuario(), estilos.metadata());
        escribirFechaHora(sheet.createRow(4).createCell(0), metadata.generadoEn(), estilos.fechaHora());
    }
    // Escribir encabezados de tabla en la fila especificada
    public void escribirEncabezados(Row row, String[] encabezados, CellStyle estilo) {
        // Escribir cada encabezado en la celda correspondiente con el estilo especificado
        for (int indice = 0; indice < encabezados.length; indice++) {
            escribirTexto(row.createCell(indice), encabezados[indice], estilo);
        }
    }
    // Escribir texto en la celda especificada con el estilo especificado
    public void escribirTexto(Cell cell, String value, CellStyle estilo) {
        // Escribir el valor de texto en la celda si no es nulo
        if (value != null) {
            cell.setCellValue(value);
        }
        cell.setCellStyle(estilo);
    }
    // Escribir entero en la celda especificada con el estilo especificado
    public void escribirEntero(Cell cell, Integer value, CellStyle estilo) {
        // Escribir el valor de entero en la celda si no es nulo
        if (value != null) {
            cell.setCellValue(value);
        }
        cell.setCellStyle(estilo);
    }
    // Escribir porcentaje en la celda especificada con el estilo especificado
    public void escribirPorcentaje(Cell cell, Integer value, CellStyle estilo) {
        // Escribir el valor de porcentaje en la celda si no es nulo, dividiendo entre 100 para obtener el formato correcto
        if (value != null) {
            cell.setCellValue(value / 100.0);
        }
        cell.setCellStyle(estilo);
    }
    // Escribir fecha y hora en la celda especificada con el estilo especificado
    public void escribirFechaHora(Cell cell, LocalDateTime value, CellStyle estilo) {
        // Escribir el valor de fecha y hora en la celda si no es nulo, convirtiendo a Date para el formato correcto
        if (value != null) {
            cell.setCellValue(Date.from(value.atZone(java.time.ZoneId.systemDefault()).toInstant()));
        }
        cell.setCellStyle(estilo);
    }
    // Configurar tabla con autofiltro y congelar paneles
    public void configurarTabla(Sheet sheet, int filaEncabezado, int ultimaFila, int ultimaColumna) {
        // Configurar autofiltro en el rango de la tabla si hay filas y columnas suficientes
        if (ultimaFila >= filaEncabezado && ultimaColumna >= 0) {
            // Configurar autofiltro en el rango de la tabla
            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(
                    filaEncabezado, ultimaFila, 0, ultimaColumna));
        }
        // Congelar paneles para que el encabezado de la tabla permanezca visible al desplazarse
        sheet.createFreezePane(0, filaEncabezado + 1);
    }
    // Ajustar el ancho de las columnas automáticamente
    public void ajustarColumnas(Sheet sheet, int cantidadColumnas) {
        // Ajustar el ancho de las columnas automáticamente para que se ajusten al contenido
        for (int indice = 0; indice < cantidadColumnas; indice++) {
            // Ajustar el ancho de la columna automáticamente
            sheet.autoSizeColumn(indice);
            // Limitar el ancho máximo de la columna a 12000 unidades para evitar que sea demasiado ancho
            int ancho = Math.min(sheet.getColumnWidth(indice) + 512, 12000);
            // Establecer el ancho de la columna al valor calculado
            sheet.setColumnWidth(indice, ancho);
        }
    }
    // Generar archivo Excel a partir de un generador de contenido
    private byte[] generarArchivo(Generador generador) {
        // Crear un archivo Excel en memoria utilizando un generador de contenido y devolverlo como un arreglo de bytes
        try (XSSFWorkbook workbook = crearWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            // Crear estilos de celdas para el workbook
            Estilos estilos = crearEstilos(workbook);
            // Generar el contenido del archivo Excel utilizando el generador proporcionado
            generador.generar(workbook, estilos);
            // Escribir el contenido del workbook en el flujo de salida
            workbook.write(output);
            // Devolver el contenido del archivo Excel como un arreglo de bytes
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("No fue posible generar el archivo Excel.", exception);
        }
    }
    // Generar reporte de operación (surtido o recepción) a partir de un DTO y metadata
    private byte[] generarOperacion(ReporteOperacionHojaDTO reporte, Metadata metadata, boolean surtido) {
        // Genera un archivo Excel con varias hojas de "Resumen", "Productos", "Movimientos" y "Contenedores" para un reporte de operación (surtido o recepción)
        return generarArchivo((workbook, estilos) -> {
            escribirResumenOperacion(workbook.createSheet("Resumen"), reporte, metadata, estilos, surtido);
            escribirProductosOperacion(workbook.createSheet("Productos"), reporte, estilos, surtido);
            escribirMovimientosOperacion(workbook.createSheet("Movimientos"), reporte, estilos);
            escribirContenedoresOperacion(workbook.createSheet("Contenedores"), reporte, estilos);
        });
    }
    // Escribir resumen de operación en la hoja especificada con los datos del reporte y metadata
    private void escribirResumenOperacion(
            Sheet sheet,
            ReporteOperacionHojaDTO reporte,
            Metadata metadata,
            Estilos estilos,
            boolean surtido) {
        // Escribir metadata en la hoja
        escribirMetadata(sheet, metadata, estilos);
        // Escribir encabezados de tabla en la fila 7
        Row encabezado = sheet.createRow(FILA_ENCABEZADO);
        // Escribir encabezados de tabla con estilo según si es surtido o recepción
        if (surtido) {
            escribirEncabezados(encabezado, new String[]{
                    "Hoja", "Proyecto", "Cliente", "Estado", "Fecha salida",
                    "Inicio surtido", "Fin surtido", "Total solicitado", "Total surtido",
                    "% Cumplimiento"
            }, estilos.encabezadoTabla());
        } else {
            escribirEncabezados(encabezado, new String[]{
                    "Hoja", "Proyecto", "Cliente", "Estado", "Fecha estimada de regreso",
                    "Inicio recepción", "Fin recepción", "Total surtido", "Total devuelto",
                    "Pendiente", "% Recepción"
            }, estilos.encabezadoTabla());
        }

        // Escribir datos de la fila de resumen
        Row row = sheet.createRow(FILA_ENCABEZADO + 1);
        // Escribir los valores de cada columna en la fila con el estilo correspondiente
        escribirEntero(row.createCell(0), reporte.getIdHoja(), estilos.entero());
        escribirTexto(row.createCell(1), reporte.getNombreProyecto(), estilos.texto());
        escribirTexto(row.createCell(2), reporte.getCliente(), estilos.texto());
        escribirTexto(row.createCell(3), reporte.getEstado(), estilos.texto());
        // Escribir las fechas y totales según si es surtido o recepción
        if (surtido) {
            escribirFechaHora(row.createCell(4), reporte.getFechaSalida(), estilos.fechaHora());
            escribirFechaHora(row.createCell(5), reporte.getInicioOperacion(), estilos.fechaHora());
            escribirFechaHora(row.createCell(6), reporte.getFinOperacion(), estilos.fechaHora());
            escribirEntero(row.createCell(7), reporte.getTotalSolicitado(), estilos.entero());
            escribirEntero(row.createCell(8), reporte.getTotalSurtido(), estilos.entero());
            escribirPorcentaje(row.createCell(9), calcularPorcentaje(
                    reporte.getTotalSurtido(), reporte.getTotalSolicitado()), estilos.porcentaje());
            configurarTabla(sheet, FILA_ENCABEZADO, FILA_ENCABEZADO + 1, 9);
            ajustarColumnas(sheet, 10);
        } else {
            escribirFechaHora(row.createCell(4), reporte.getFechaEstimadaRegreso(), estilos.fechaHora());
            escribirFechaHora(row.createCell(5), reporte.getInicioOperacion(), estilos.fechaHora());
            escribirFechaHora(row.createCell(6), reporte.getFinOperacion(), estilos.fechaHora());
            escribirEntero(row.createCell(7), reporte.getTotalSurtido(), estilos.entero());
            escribirEntero(row.createCell(8), reporte.getTotalDevuelto(), estilos.entero());
            escribirEntero(row.createCell(9), calcularPendiente(
                    reporte.getTotalSurtido(), reporte.getTotalDevuelto()), estilos.entero());
            escribirPorcentaje(row.createCell(10), calcularPorcentaje(
                    reporte.getTotalDevuelto(), reporte.getTotalSurtido()), estilos.porcentaje());
            configurarTabla(sheet, FILA_ENCABEZADO, FILA_ENCABEZADO + 1, 10);
            ajustarColumnas(sheet, 11);
        }
    }
    // Escribir productos de operación en la hoja especificada con los datos del reporte y metadata
    private void escribirProductosOperacion(
            Sheet sheet,
            ReporteOperacionHojaDTO reporte,
            Estilos estilos,
            boolean surtido) {
        // Escribir encabezados de tabla en la fila 0 según si es surtido o recepción
        escribirEncabezados(sheet.createRow(0), surtido
                ? new String[]{"Producto", "Solicitado", "Surtido", "Pendiente"}
                : new String[]{"Producto", "Surtido", "Devuelto", "Pendiente"},
                estilos.encabezadoTabla());
        // Escribir datos de los productos a partir de la fila 1
        int fila = 1;
        // Iterar sobre los detalles del reporte y escribir cada uno en una fila
        for (ReporteOperacionDetalleDTO detalle : listaSegura(reporte.getDetalles())) {
            // Crear una nueva fila en la hoja
            Row row = sheet.createRow(fila++);
            // Escribir los valores de cada columna en la fila con el estilo correspondiente según si es surtido o recepción
            escribirTexto(row.createCell(0), detalle.getProducto(), estilos.texto());
            // Escribir los valores de cantidad solicitada, surtida y pendiente si es surtido
            if (surtido) {
                escribirEntero(row.createCell(1), detalle.getCantidadSolicitada(), estilos.entero());
                escribirEntero(row.createCell(2), detalle.getCantidadSurtida(), estilos.entero());
                escribirEntero(row.createCell(3), calcularPendiente(
                        detalle.getCantidadSolicitada(), detalle.getCantidadSurtida()), estilos.entero());
            } else {
                escribirEntero(row.createCell(1), detalle.getCantidadSurtida(), estilos.entero());
                escribirEntero(row.createCell(2), detalle.getCantidadDevuelta(), estilos.entero());
                escribirEntero(row.createCell(3), calcularPendiente(
                        detalle.getCantidadSurtida(), detalle.getCantidadDevuelta()), estilos.entero());
            }
        }
        // Configurar tabla y ajustar columnas
        configurarTabla(sheet, 0, fila - 1, 3);
        ajustarColumnas(sheet, 4);
    }
    // Escribir movimientos de operación en la hoja especificada con los datos del reporte y metadata
    private void escribirMovimientosOperacion(
            Sheet sheet,
            ReporteOperacionHojaDTO reporte,
            Estilos estilos) {
        // Escribir encabezados de tabla en la fila 0
        escribirEncabezados(sheet.createRow(0), new String[]{
                "Fecha", "Conjunto", "Producto", "Contenedor", "Usuario", "Observaciones"
        }, estilos.encabezadoTabla());
        // Escribir datos de los movimientos a partir de la fila 1
        int fila = 1;
        // Iterar sobre los movimientos del reporte y escribir cada uno en una fila
        for (ReporteMovimientoDTO movimiento : listaSegura(reporte.getMovimientos())) {
            // Crear una nueva fila en la hoja
            Row row = sheet.createRow(fila++);
            // Escribir los valores de cada columna en la fila con el estilo correspondiente
            escribirFechaHora(row.createCell(0), movimiento.getFecha(), estilos.fechaHora());
            // Escribir el código del conjunto, producto, contenedor, usuario y observaciones en la fila
            escribirTexto(row.createCell(1), movimiento.getCodigoConjunto(), estilos.texto());
            escribirTexto(row.createCell(2), movimiento.getProducto(), estilos.texto());
            escribirTexto(row.createCell(3), movimiento.getCodigoContenedor() == null
                    ? "Sin contenedor" : movimiento.getCodigoContenedor(), estilos.texto());
            escribirTexto(row.createCell(4), movimiento.getUsuario(), estilos.texto());
            escribirTexto(row.createCell(5), movimiento.getObservaciones(), estilos.observaciones());
        }
        // Configurar tabla y ajustar columnas
        configurarTabla(sheet, 0, fila - 1, 5);
        ajustarColumnas(sheet, 6);
    }
    // Escribir contenedores de operación en la hoja especificada con los datos del reporte y metadata
    private void escribirContenedoresOperacion(
            Sheet sheet,
            ReporteOperacionHojaDTO reporte,
            Estilos estilos) {
        // Escribir encabezados de tabla en la fila 0
        escribirEncabezados(sheet.createRow(0), new String[]{
                "Contenedor", "Capacidad", "Salidas", "% Utilización", "Fecha asignación",
                "Usuario asignación", "Fecha cierre", "Usuario cierre", "Fecha liberación",
                "Usuario liberación"
        }, estilos.encabezadoTabla());
        // Escribir datos de los contenedores a partir de la fila 1
        int fila = 1;
        // Iterar sobre los contenedores del reporte y escribir cada uno en una fila
        for (ReporteContenedorDTO contenedor : listaSegura(reporte.getContenedores())) {
            // Crear una nueva fila en la hoja
            Row row = sheet.createRow(fila++);
            // Escribir los valores de cada columna en la fila con el estilo correspondiente
            escribirTexto(row.createCell(0), contenedor.getCodigoContenedor(), estilos.texto());
            escribirEntero(row.createCell(1), contenedor.getCapacidad(), estilos.entero());
            escribirEntero(row.createCell(2), contenedor.getCantidadSalidas(), estilos.entero());
            escribirPorcentaje(row.createCell(3), contenedor.getPorcentajeUtilizacion(), estilos.porcentaje());
            escribirFechaHora(row.createCell(4), contenedor.getFechaAsignacion(), estilos.fechaHora());
            escribirTexto(row.createCell(5), contenedor.getUsuarioAsignacion(), estilos.texto());
            escribirFechaHora(row.createCell(6), contenedor.getFechaCierreCarga(), estilos.fechaHora());
            escribirTexto(row.createCell(7), contenedor.getUsuarioCierre(), estilos.texto());
            escribirFechaHora(row.createCell(8), contenedor.getFechaLiberacion(), estilos.fechaHora());
            escribirTexto(row.createCell(9), contenedor.getUsuarioLiberacion(), estilos.texto());
        }
        // Configurar tabla y ajustar columnas
        configurarTabla(sheet, 0, fila - 1, 9);
        ajustarColumnas(sheet, 10);
    }
    // Calcular pendiente entre total y realizado, asegurando que no sea negativo
    private int calcularPendiente(Integer total, Integer realizado) {
        return Math.max(valor(total) - valor(realizado), 0);
    }
    // Calcular porcentaje de realizado sobre total, asegurando que no haya división por cero
    private int calcularPorcentaje(Integer realizado, Integer total) {
        int divisor = valor(total);
        if (divisor <= 0) {
            return 0;
        }
        return (int) Math.round((valor(realizado) * 100.0) / divisor);
    }
    // Devolver el valor de un Integer, o 0 si es nulo
    private int valor(Integer value) {
        return value == null ? 0 : value;
    }
    // Devolver una lista segura, evitando null y devolviendo una lista vacía si es nula
    private <T> List<T> listaSegura(List<T> values) {
        return values == null ? List.of() : values;
    }
    // Interfaz funcional para generar contenido en un workbook con estilos
    @FunctionalInterface
    private interface Generador {
        void generar(XSSFWorkbook workbook, Estilos estilos);
    }
    // Registro de metadata para reportes, incluyendo título, bodega, usuario, fecha de generación y filtros aplicados
    public record Metadata(
            String titulo,
            String bodega,
            String usuario,
            LocalDateTime generadoEn,
            String filtros) {
    }
    // Registro de estilos de celdas para reportes, incluyendo estilos para título, metadata, encabezado de tabla, texto, enteros, porcentajes, fechas y observaciones
    public record Estilos(
            CellStyle titulo,
            CellStyle metadata,
            CellStyle encabezadoTabla,
            CellStyle texto,
            CellStyle entero,
            CellStyle porcentaje,
            CellStyle fecha,
            CellStyle fechaHora,
            CellStyle observaciones) {
    }
}
