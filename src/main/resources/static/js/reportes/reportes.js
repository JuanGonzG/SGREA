(function () {
    // Usar strict mode para evitar errores silenciosos y mejorar la depuración
    'use strict';
    // Mensaje que se muestra cuando no hay registros en la tabla
    const PLACEHOLDER = 'No se encontraron registros con los filtros seleccionados.';
    // Objeto que almacena las instancias de Tabulator para cada reporte
    const tablas = {};
    // Conjunto que almacena los nombres de los reportes que ya han sido cargados
    const cargadas = new Set();
    // Definiciones de los reportes, incluyendo formulario, endpoint, tabla y columnas
    const definiciones = {
        hojas: {
            // Definición del reporte de hojas
            form: 'filtrosHojas', endpoint: '/reportes/api/hojas', excelEndpoint: '/reportes/hojas/excel', tabla: 'tablaReportesHojas', mensaje: 'Cargando hojas...',
            // Definición de las columnas de la tabla
            columnas: [
                {title: 'Hoja', field: 'idHoja', width: 85, hozAlign: 'center'},
                {title: 'Proyecto', field: 'nombreProyecto', minWidth: 180},
                {title: 'Cliente', field: 'cliente', minWidth: 160},
                {title: 'Estado', field: 'estado', minWidth: 130},
                {title: 'Salida', field: 'fechaSalida', formatter: fechaHora},
                {title: 'Regreso estimado', field: 'fechaEstimadaRegreso', formatter: fechaHora},
                {title: 'Solicitado', field: 'totalSolicitado', hozAlign: 'center'},
                {title: 'Surtido', field: 'totalSurtido', hozAlign: 'center'},
                {title: 'Devuelto', field: 'totalDevuelto', hozAlign: 'center'},
                {title: '% Surtido', field: 'porcentajeSurtido', formatter: porcentaje},
                {title: '% Recepción', field: 'porcentajeRecepcion', formatter: porcentaje},
                {title: 'Detalle', field: 'idHoja', formatter: detalleHoja, headerSort: false, minWidth: 155}
            ]
        },
        // Definición del reporte de movimientos
        movimientos: {
            form: 'filtrosMovimientos', endpoint: '/reportes/api/movimientos', excelEndpoint: '/reportes/movimientos/excel', tabla: 'tablaReportesMovimientos', mensaje: 'Cargando movimientos...',
            // Definición de las columnas de la tabla
            columnas: [
                {title: 'Fecha', field: 'fecha', formatter: fechaHora},
                {title: 'Tipo', field: 'tipoMovimiento', formatter: tipoMovimiento},
                {title: 'Hoja', field: 'idHoja', hozAlign: 'center'},
                {title: 'Proyecto', field: 'proyecto', minWidth: 180},
                {title: 'Conjunto', field: 'codigoConjunto'},
                {title: 'Producto', field: 'producto', minWidth: 160},
                {title: 'Contenedor', field: 'codigoContenedor', formatter: contenedor},
                {title: 'Usuario', field: 'usuario'},
                {title: 'Observaciones', field: 'observaciones', minWidth: 180}
            ]
        },
        // Definición del reporte de contenedores
        contenedores: {
            form: 'filtrosContenedores', endpoint: '/reportes/api/contenedores', excelEndpoint: '/reportes/contenedores/excel', tabla: 'tablaReportesContenedores', mensaje: 'Cargando contenedores...',
            // Definición de las columnas de la tabla
            columnas: [
                {title: 'Contenedor', field: 'codigoContenedor'},
                {title: 'Hoja', field: 'idHoja', hozAlign: 'center'},
                {title: 'Proyecto', field: 'proyecto', minWidth: 180},
                {title: 'Capacidad', field: 'capacidad', hozAlign: 'center'},
                {title: 'Salidas', field: 'cantidadSalidas', hozAlign: 'center'},
                {title: 'Utilización', field: 'porcentajeUtilizacion', formatter: porcentaje},
                {title: 'Fecha asignación', field: 'fechaAsignacion', formatter: fechaHora},
                {title: 'Usuario asignación', field: 'usuarioAsignacion'},
                {title: 'Fecha cierre', field: 'fechaCierreCarga', formatter: fechaHora},
                {title: 'Usuario cierre', field: 'usuarioCierre'},
                {title: 'Fecha liberación', field: 'fechaLiberacion', formatter: fechaHora},
                {title: 'Usuario liberación', field: 'usuarioLiberacion'}
            ]
        },
        // Definición del reporte de inventario
        inventario: {
            form: 'filtrosInventario', endpoint: '/reportes/api/conjuntos', excelEndpoint: '/reportes/conjuntos/excel', tabla: 'tablaReportesInventario', mensaje: 'Cargando inventario...',
            // Definición de las columnas de la tabla
            columnas: [
                {title: 'Conjunto', field: 'codigoConjunto'},
                {title: 'Producto', field: 'producto', minWidth: 180},
                {title: 'Estado', field: 'estado'},
                {title: 'Fecha alta', field: 'fechaAlta', formatter: fechaHora},
                {title: 'Observaciones', field: 'observaciones', minWidth: 220}
            ]
        }
    };
    // Función para formatear fechas y horas en formato local de México
    function fechaHora(cell) {
        const valor = cell.getValue();
        if (!valor) return '';
        const fecha = new Date(valor);
        return Number.isNaN(fecha.getTime()) ? valor : fecha.toLocaleString('es-MX');
    }

    // Función para formatear valores como porcentaje
    function porcentaje(cell) {
        const valor = cell.getValue();
        return valor === null || valor === undefined ? '' : `${valor}%`;
    }

    // Función para formatear el valor del contenedor
    function contenedor(cell) {
        return cell.getValue() || 'Sin contenedor';
    }

    // Función para formatear el tipo de movimiento
    function tipoMovimiento(cell) {
        const valor = cell.getValue() || '';
        const clase = valor.toLowerCase() === 'salida' ? 'text-bg-primary' : 'text-bg-success';
        return `<span class="badge ${clase}">${escapeHtml(valor)}</span>`;
    }

    // Función para formatear el detalle de la hoja
    function detalleHoja(cell) {
        // Obtener el valor de la celda que contiene el ID de la hoja
        const id = cell.getValue();
        if (!id) return '';
        // Retornar un bloque HTML con botones para ver el surtido, la recepción y exportar a PDF
        return `<div class="d-flex flex-wrap gap-1">
            <a class="btn btn-sm btn-outline-primary" href="/reportes/hojas/${encodeURIComponent(id)}/surtido">Surtido</a>
            <a class="btn btn-sm btn-outline-success" href="/reportes/hojas/${encodeURIComponent(id)}/recepcion">Recepción</a>
            <button class="btn btn-sm btn-outline-danger" type="button" data-export-pdf="hoja" data-id-hoja="${encodeURIComponent(id)}" title="Exportar Hoja a PDF">PDF</button>
        </div>`;
    }
    // Función para escapar caracteres especiales en HTML
    function escapeHtml(valor) {
        return String(valor ?? '').replace(/[&<>'"]/g, caracter => ({'&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;'}[caracter]));
    }
    // Función para obtener los parámetros del formulario como URLSearchParams
    function parametrosFormulario(formulario) {
        // Crear un objeto URLSearchParams a partir de los datos del formulario
        const params = new URLSearchParams();
        // Iterar sobre los datos del formulario y agregar solo los valores no vacíos
        new FormData(formulario).forEach((valor, nombre) => {
            const texto = String(valor).trim();
            if (texto) params.append(nombre, texto);
        });
        // Retornar los parámetros del formulario
        return params;
    }
    // Función para obtener los datos del reporte desde el endpoint correspondiente
    async function obtenerDatos(definicion) {
        // Obtener los parámetros del formulario y construir la URL del endpoint
        const query = parametrosFormulario(document.getElementById(definicion.form));
        const url = query.toString() ? `${definicion.endpoint}?${query}` : definicion.endpoint;
        // Realizar la solicitud fetch al endpoint con los encabezados adecuados
        const respuesta = await fetch(url, {headers: {'Accept': 'application/json'}});
        let cuerpo = null;
        // Intentar parsear la respuesta como JSON, si falla, se asume que no hay JSON en la respuesta
        try { cuerpo = await respuesta.json(); } catch (_) { /* respuesta sin JSON */ }
        // Manejar los diferentes códigos de estado HTTP y lanzar errores con mensajes apropiados
        if (respuesta.status === 401) {
            window.location.href = '/login';
            throw new Error('La sesión ha expirado.');
        }
        // Manejar el caso de error 403 (Forbidden) indicando que no hay permisos para consultar reportes
        if (respuesta.status === 403) throw new Error('No tienes permisos para consultar Reportes.');
        if (respuesta.status === 400) throw new Error(cuerpo?.mensaje || 'Los filtros no son válidos.');
        if (!respuesta.ok) throw new Error('No fue posible consultar el reporte.');
        return Array.isArray(cuerpo) ? cuerpo : [];
    }
    // Función para cargar los datos del reporte y actualizar la tabla correspondiente
    async function cargar(nombre) {
        // Obtener la definición del reporte según el nombre proporcionado
        const definicion = definiciones[nombre];
        // Si no existe la definición, salir de la función
        if (!definicion) return;
        LoadingOverlay.mostrar(definicion.mensaje);
        try {
            // Obtener los datos del reporte desde el endpoint correspondiente
            const datos = await obtenerDatos(definicion);
            // Si la tabla aún no ha sido creada, crear una nueva instancia de Tabulator con los datos obtenidos
            if (!tablas[nombre]) {
                // Crear una nueva instancia de Tabulator para la tabla correspondiente
                tablas[nombre] = new Tabulator(`#${definicion.tabla}`, {
                    data: datos,
                    layout: 'fitDataStretch',
                    responsiveLayout: false,
                    placeholder: PLACEHOLDER,
                    columns: definicion.columnas,
                    columnDefaults: {vertAlign: 'middle', headerHozAlign: 'center'},
                    locale: 'es',
                    langs: {es: {data: {loading: 'Cargando...', error: 'Error al cargar'}}}
                });
            } else {
                // Si la tabla ya existe, reemplazar los datos existentes con los nuevos datos obtenidos
                await tablas[nombre].replaceData(datos);
            }
            // Marcar el reporte como cargado para evitar recargas innecesarias
            cargadas.add(nombre);
        } catch (error) {
            mostrarError(error.message || 'No fue posible consultar el reporte.', 'Error de consulta');
        } finally {
            await LoadingOverlay.ocultar();
        }
    }
    // Función para conectar el formulario de filtros con la carga de datos del reporte
    function conectarFormulario(nombre) {
        // Obtener la definición del reporte según el nombre proporcionado
        const definicion = definiciones[nombre];
        // Obtenemos el formulario correspondiente al reporte y agregamos un listener para el evento submit
        const formulario = document.getElementById(definicion.form);
        formulario?.addEventListener('submit', event => {
            event.preventDefault();
            cargar(nombre);
        });
        // Agregamos un listener para el botón de limpiar filtros, que resetea el formulario y recarga los datos del reporte
        formulario?.querySelector('[data-limpiar]')?.addEventListener('click', () => {
            formulario.reset();
            cargar(nombre);
        });
    }
    // Función para conectar los tabs de los reportes y cargar los datos correspondientes al cambiar de tab
    function conectarTabs() {
        // Agregamos un listener para cada tab de los reportes, que se activa cuando se muestra un tab
        document.querySelectorAll('#tabsReportes button[data-bs-toggle="tab"]').forEach(tab => {
            // Cuando se muestra un tab, obtenemos el nombre del reporte correspondiente y cargamos los datos si aún no se han cargado
            tab.addEventListener('shown.bs.tab', event => {
                const nombre = event.target.id.replace('tab', '').toLowerCase();
                if (definiciones[nombre] && !cargadas.has(nombre)) cargar(nombre);
            });
        });
    }

    // Función para conectar los botones de exportación de los reportes
    function conectarExportaciones() {
        // Agregamos un listener para cada botón de exportación a Excel, que se activa al hacer clic en el botón
        document.querySelectorAll('[data-export-excel]').forEach(boton => {
            // Cuando se hace clic en el botón, obtenemos el nombre del reporte correspondiente y descargamos el archivo Excel si la definición existe
            boton.addEventListener('click', () => {
                // Obtenemos el nombre del reporte desde el atributo data-export-excel del botón
                const nombre = boton.dataset.exportExcel;
                // Obtenemos la definición del reporte correspondiente al nombre obtenido
                const definicion = definiciones[nombre];
                if (!definicion || !window.ReportesExcel) return;
                // Obtenemos el formulario correspondiente al reporte y descargamos el archivo Excel con los parámetros del formulario
                const formulario = document.getElementById(definicion.form);
                ReportesExcel.descargar(
                    definicion.excelEndpoint,
                    parametrosFormulario(formulario),
                    `SGREA_${nombre}.xlsx`
                );
            });
        });
        // Agregamos un listener para el evento click en el documento, que se activa al hacer clic en cualquier parte del documento
        document.addEventListener('click', event => {
            // Obtenemos el botón más cercano al elemento clickeado que tenga el atributo data-export-pdf="hoja"
            const boton = event.target.closest('[data-export-pdf="hoja"]');
            // Si no se encuentra el botón o no existe la función ReportesPdf, salimos de la función
            if (!boton || !window.ReportesPdf) return;
            const id = boton.dataset.idHoja;
            if (!id) return;
            // Llamamos a la función descargar de ReportesPdf para generar y descargar el PDF de la hoja correspondiente al ID obtenido
            ReportesPdf.descargar(
                `/reportes/hojas/${id}/pdf`,
                null,
                `SGREA_Hoja_${id}.pdf`,
                'Generando PDF de la Hoja...'
            );
        });
    }
    // Agregamos un listener para el evento DOMContentLoaded, que se activa cuando el contenido del DOM ha sido cargado
    document.addEventListener('DOMContentLoaded', () => {
        // Iteramos sobre las claves del objeto definiciones y conectamos cada formulario de filtros con la carga de datos del reporte correspondiente
        Object.keys(definiciones).forEach(conectarFormulario);
        // Conectamos los tabs de los reportes para cargar los datos correspondientes al cambiar de tab
        conectarTabs();
        // Conectamos los botones de exportación de los reportes para descargar archivos Excel y PDF
        conectarExportaciones();
        // Cargamos los datos del reporte de hojas al iniciar la página
        cargar('hojas');
    });
})();
