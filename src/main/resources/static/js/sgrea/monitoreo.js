// Variables de estado para evitar múltiples solicitudes simultáneas
let monitoreoDashboardEnProceso = false;
let monitoreoDetalleEnProceso = false;
let monitoreoIntervalo = null;
let monitoreoErrorNotificado = false;
let tablaMonitoreoSurtidos = null;
let tablaMonitoreoRecepciones = null;
// Intervalo de actualización en milisegundos (5 segundos)
const MONITOREO_INTERVALO_MS = 5000;
// Funciones de utilidad para formatear y escapar valores
function escaparHtmlMonitoreo(valor) {
    const caracteres = {'&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;'};
    return String(valor ?? '').replace(/[&<>"']/g, caracter => caracteres[caracter]);
}
// Función para formatear fechas en formato local
function formatearFechaMonitoreo(valor) {
    if (!valor) return 'Sin actividad';
    const fecha = new Date(valor);
    if (Number.isNaN(fecha.getTime())) return 'Fecha no disponible';
    return fecha.toLocaleString('es-MX', {dateStyle: 'short', timeStyle: 'short'});
}
// Función para obtener el texto del estado de monitoreo
function textoEstadoMonitoreo(valor) {
    return escaparHtmlMonitoreo(valor || 'Sin estado');
}
// Función para obtener el porcentaje de monitoreo, asegurando que esté entre 0 y 100
function porcentajeMonitoreo(valor) {
    const numero = Number(valor);
    return Number.isFinite(numero) ? Math.max(0, Math.min(100, numero)) : 0;
}
// Función para leer la respuesta de la API de monitoreo y manejar errores
async function leerRespuestaMonitoreo(response, mensajePredeterminado) {
    let data = null;
    // Intentar parsear la respuesta como JSON, si falla, asignar null
    try {
        data = await response.json();
    } catch (_) {
        data = null;
    }
    // Manejar errores de autenticación y autorización
    if (response.status === 401) {
        window.location.href = '/login';
        throw new Error('La sesión ha expirado.');
    }
    if (response.status === 403) {
        if (!monitoreoErrorNotificado) {
            monitoreoErrorNotificado = true;
            if (typeof mostrarError === 'function') {
                mostrarError(data?.mensaje || 'No tienes permisos para consultar Monitoreo.', 'Acceso denegado');
            }
        }
        throw new Error(data?.mensaje || 'Acceso denegado.');
    }
    if (!response.ok) {
        throw new Error(data?.mensaje || mensajePredeterminado);
    }
    // Restablecer el estado de error notificado
    monitoreoErrorNotificado = false;
    return data;
}
// Función para crear una tabla de monitoreo usando Tabulator
function crearTablaMonitoreo(contenedor, datos, tipo) {
    if (!contenedor || typeof Tabulator === 'undefined') return null;
    // Determinar si es una tabla de surtidos o recepciones
    const esSurtido = tipo === 'surtido';
    // Configurar la tabla con las opciones de Tabulator
    return new Tabulator(contenedor, {
        data: datos,
        layout: 'fitColumns',
        pagination: true,
        paginationSize: 10,
        paginationSizeSelector: [10, 25, 50],
        placeholder: esSurtido ? 'No hay surtidos activos.' : 'No hay recepciones activas.',
        // Manejar el clic en una fila para redirigir al detalle correspondiente
        rowClick: (_, row) => {
            const idHoja = row.getData().idHoja;
            window.location.href = `/monitoreo/${esSurtido ? 'surtido' : 'recepcion'}/${encodeURIComponent(idHoja)}`;
        },
        columnDefaults: {
            vertAlign: 'middle',
            headerHozAlign: 'center'
        },
        // Definir las columnas de la tabla con sus títulos, campos y formatos
        columns: [
            {title: 'Hoja', field: 'idHoja', width: 85, hozAlign: 'center'},
            {title: 'Proyecto', field: 'nombreProyecto', headerFilter: 'input', minWidth: 180,
                formatter: cell => `<strong>${escaparHtmlMonitoreo(cell.getValue())}</strong>`},
            {title: 'Cliente', field: 'cliente', headerFilter: 'input', minWidth: 150,
                formatter: cell => escaparHtmlMonitoreo(cell.getValue())},
            {title: 'Estado', field: 'estado', minWidth: 130,
                formatter: cell => `<span class="badge text-bg-secondary">${textoEstadoMonitoreo(cell.getValue())}</span>`},
            {title: 'Avance', field: 'porcentaje', width: 150,
                formatter: cell => {
                    const valor = porcentajeMonitoreo(cell.getValue());
                    const color = esSurtido ? 'bg-primary' : 'bg-info';
                    return `<div class="small fw-semibold">${valor}%</div><div class="progress" role="progressbar" aria-valuenow="${valor}" aria-valuemin="0" aria-valuemax="100"><div class="progress-bar ${color}" style="width:${valor}%"></div></div>`;
                }},
            {title: 'Pendiente', field: 'pendiente', width: 105, hozAlign: 'center'},
            {title: 'Contenedores pendientes', field: 'contenedoresPendientes', width: 170, hozAlign: 'center'},
            {title: 'Último operador', field: 'ultimoOperador', minWidth: 150,
                formatter: cell => escaparHtmlMonitoreo(cell.getValue() || 'Sin actividad')},
            {title: 'Última actividad', field: 'ultimaActividad', minWidth: 165,
                formatter: cell => formatearFechaMonitoreo(cell.getValue())},
            {title: 'Detalle', width: 105, hozAlign: 'center', headerSort: false,
                formatter: cell => {
                    const idHoja = encodeURIComponent(cell.getRow().getData().idHoja);
                    const tipoDetalle = esSurtido ? 'surtido' : 'recepcion';
                    return `<a class="btn btn-sm btn-outline-primary" href="/monitoreo/${tipoDetalle}/${idHoja}">Ver</a>`;
                },
                cellClick: (evento) => evento.stopPropagation()}
        ],
        locale: 'es',
        langs: {es: {data: {loading: 'Cargando...', error: 'Error al cargar'}, pagination: {
            page_size: 'Filas por página', first: 'Primera', last: 'Última', prev: 'Anterior', next: 'Siguiente', all: 'Todas'
        }, headerFilters: {default: 'Filtrar columna...'}}}
    });
}
// Función para inicializar las tablas de monitoreo si existen en el DOM
function inicializarTablasMonitoreo() {
    // Obtener los elementos de las tablas de surtidos y recepciones
    const surtidos = document.getElementById('tablaMonitoreoSurtidos');
    const recepciones = document.getElementById('tablaMonitoreoRecepciones');
    // Si no existen ambos elementos, no hacer nada
    if (!surtidos && !recepciones) return;
    // Crear las tablas de monitoreo usando Tabulator
    tablaMonitoreoSurtidos = crearTablaMonitoreo(surtidos, [], 'surtido');
    tablaMonitoreoRecepciones = crearTablaMonitoreo(recepciones, [], 'recepcion');
}
// Función para actualizar los datos de una tabla de monitoreo
function actualizarTablaMonitoreo(tabla, datos) {
    if (tabla) {
        tabla.setData(datos || []);
    }
}
// Función para cargar los datos del dashboard de monitoreo
async function cargarDashboardMonitoreo() {
    // Evitar múltiples solicitudes simultáneas
    if (monitoreoDashboardEnProceso) return;
    monitoreoDashboardEnProceso = true;
    // Realizar solicitudes concurrentes para obtener surtidos y recepciones
    try {
        const [surtidosResponse, recepcionesResponse] = await Promise.all([
            fetch('/monitoreo/api/surtidos'),
            fetch('/monitoreo/api/recepciones')
        ]);
        // Leer las respuestas y manejar errores
        const surtidos = await leerRespuestaMonitoreo(surtidosResponse, 'No fue posible consultar los surtidos.');
        const recepciones = await leerRespuestaMonitoreo(recepcionesResponse, 'No fue posible consultar las recepciones.');
        // Actualizar las tablas de monitoreo con los datos obtenidos
        actualizarTablaMonitoreo(tablaMonitoreoSurtidos, surtidos);
        actualizarTablaMonitoreo(tablaMonitoreoRecepciones, recepciones);
        // Actualizar la fecha de la última actualización en el dashboard
        const ultimaActualizacion = document.getElementById('monitoreoUltimaActualizacion');
        // Formatear la fecha actual en formato ISO y mostrarla en el elemento correspondiente
        if (ultimaActualizacion) ultimaActualizacion.textContent = formatearFechaMonitoreo(new Date().toISOString());
    } catch (error) {
        if (error.message !== 'La sesión ha expirado.' && error.message !== 'Acceso denegado.' && typeof mostrarError === 'function') {
            mostrarError(error.message, 'No fue posible actualizar Monitoreo');
        }
    } finally {
        monitoreoDashboardEnProceso = false;
    }
}
// Función para colocar texto en un elemento del DOM por su ID
function colocarTextoMonitoreo(id, valor) {
    const elemento = document.getElementById(id);
    if (elemento) elemento.textContent = valor ?? '';
}
// Función para renderizar los detalles de monitoreo en un contenedor específico
function renderizarDetallesMonitoreo(detalles, idContenedor, tipo) {
    // Obtener el contenedor por su ID
    const contenedor = document.getElementById(idContenedor);
    if (!contenedor) return;
    // Si no hay detalles, mostrar un mensaje de placeholder
    if (!detalles?.length) {
        contenedor.innerHTML = '<div class="monitoreo-placeholder">No hay detalles registrados.</div>';
        return;
    }
    // Determinar si es un monitoreo de recepción o surtido
    const recepcion = tipo === 'recepcion';
    // Renderizar la lista de detalles con su porcentaje, cantidad procesada y pendiente
    contenedor.innerHTML = `<ul class="list-group list-group-flush">${detalles.map(detalle => {
        const porcentaje = porcentajeMonitoreo(recepcion ? detalle.porcentajeRecepcion : detalle.porcentajeSurtido);
        const procesado = recepcion ? detalle.cantidadDevuelta : detalle.cantidadSurtida;
        const pendiente = recepcion ? detalle.pendienteDevolver : detalle.pendienteSurtir;
        // Construir el HTML para cada detalle con su información y barra de progreso
        return `<li class="list-group-item px-0">
            <div class="d-flex justify-content-between gap-2">
                <strong>${escaparHtmlMonitoreo(detalle.producto || 'Producto')}</strong>
                <span class="small fw-semibold">${porcentaje}%</span>
            </div>
            <div class="small text-muted mt-1">Procesado: ${procesado ?? 0} · Pendiente: ${pendiente ?? 0}</div>
            <div class="progress mt-2" role="progressbar" aria-valuenow="${porcentaje}" aria-valuemin="0" aria-valuemax="100">
                <div class="progress-bar ${recepcion ? 'bg-info' : ''}" style="width:${porcentaje}%"></div>
            </div>
        </li>`;
    }).join('')}</ul>`;
}

// Función para renderizar los contenedores de monitoreo en un contenedor específico
function renderizarContenedoresMonitoreo(contenedores, idContenedor) {
    // Obtener el contenedor por su ID
    const contenedor = document.getElementById(idContenedor);
    if (!contenedor) return;
    // Si no hay contenedores, mostrar un mensaje de placeholder
    if (!contenedores?.length) {
        contenedor.innerHTML = '<div class="monitoreo-placeholder">No hay utilizaciones de Contenedor.</div>';
        return;
    }
    // Renderizar la lista de contenedores con su código, estado de liberación y ocupación histórica
    contenedor.innerHTML = `<ul class="list-group list-group-flush">${contenedores.map(item => `<li class="list-group-item px-0">
        <div class="d-flex justify-content-between gap-2">
            <strong>${escaparHtmlMonitoreo(item.codigo || 'Contenedor')}</strong>
            <span class="badge ${item.liberado ? 'text-bg-success' : 'text-bg-warning'}">${item.liberado ? 'Liberado' : 'Pendiente'}</span>
        </div>
        <div class="small text-muted mt-1">Ocupación histórica: ${item.ocupacion ?? 0} / ${item.capacidad ?? 0}</div>
        <div class="small text-muted">Asignado: ${formatearFechaMonitoreo(item.fechaAsignacion)}</div>
        <div class="small text-muted">${item.fechaLiberacion ? `Liberado: ${formatearFechaMonitoreo(item.fechaLiberacion)}` : 'Sin liberación registrada'}</div>
    </li>`).join('')}</ul>`;
}
// Función para renderizar los movimientos de monitoreo en un contenedor específico
function renderizarMovimientosMonitoreo(movimientos, idContenedor) {
    // Obtener el contenedor por su ID
    const contenedor = document.getElementById(idContenedor);
    if (!contenedor) return;
    // Si no hay movimientos, mostrar un mensaje de placeholder
    if (!movimientos?.length) {
        contenedor.innerHTML = '<div class="monitoreo-placeholder">No hay movimientos registrados.</div>';
        return;
    }
    // Renderizar la lista de movimientos con su código de conjunto, fecha, tipo de movimiento, producto y usuario
    contenedor.innerHTML = `<ul class="list-group list-group-flush">${movimientos.map(movimiento => `<li class="list-group-item px-0">
        <div class="d-flex justify-content-between gap-2"><strong>${escaparHtmlMonitoreo(movimiento.codigoConjunto)}</strong><span class="small text-muted">${formatearFechaMonitoreo(movimiento.fecha)}</span></div>
        <div class="small text-muted">${escaparHtmlMonitoreo(movimiento.tipoMovimiento)} · ${escaparHtmlMonitoreo(movimiento.producto)} · ${escaparHtmlMonitoreo(movimiento.usuario)}</div>
        <div class="small text-muted">${escaparHtmlMonitoreo(movimiento.codigoContenedor || 'Sin contenedor')}</div>
    </li>`).join('')}</ul>`;
}
// Función para renderizar el detalle de monitoreo en la vista de detalle
function renderizarDetalleMonitoreo(estado, tipo) {
    // Determinar si es un monitoreo de recepción o surtido
    const recepcion = tipo === 'recepcion';
    // Construir el prefijo para los IDs de los elementos según el tipo de monitoreo
    const prefijo = recepcion ? 'monitoreoRecepcion' : 'monitoreoSurtido';
    // Colocar los textos en los elementos correspondientes usando el prefijo y los valores del estado
    colocarTextoMonitoreo(`${prefijo}Proyecto`, estado.nombreProyecto || 'Proyecto');
    colocarTextoMonitoreo(`${prefijo}Cliente`, estado.cliente || 'Sin cliente');
    colocarTextoMonitoreo(`${prefijo}Estado`, estado.estado || 'Sin estado');
    colocarTextoMonitoreo(`${prefijo}Porcentaje`, recepcion ? estado.porcentajeRecepcion : estado.porcentajeSurtido);
    // Actualizar la barra de progreso con el porcentaje correspondiente
    const progreso = document.getElementById(`${prefijo}Progreso`);
    if (progreso) {
        const valor = porcentajeMonitoreo(recepcion ? estado.porcentajeRecepcion : estado.porcentajeSurtido);
        progreso.style.width = `${valor}%`;
        progreso.textContent = `${valor}%`;
        progreso.parentElement.setAttribute('aria-valuenow', valor);
    }
    // Colocar los textos de totales y pendientes según el tipo de monitoreo
    colocarTextoMonitoreo(recepcion ? 'monitoreoRecepcionTotalSurtido' : 'monitoreoTotalSolicitado', recepcion ? estado.totalSurtido : estado.totalSolicitado);
    colocarTextoMonitoreo(recepcion ? 'monitoreoTotalDevuelto' : 'monitoreoTotalSurtido', recepcion ? estado.totalDevuelto : estado.totalSurtido);
    colocarTextoMonitoreo(recepcion ? 'monitoreoPendienteDevolver' : 'monitoreoPendienteSurtir', recepcion ? estado.pendienteDevolver : estado.pendienteSurtir);
    // Renderizar los detalles, contenedores y movimientos en sus respectivos contenedores
    renderizarDetallesMonitoreo(estado.detalles, recepcion ? 'monitoreoDetallesRecepcion' : 'monitoreoDetallesSurtido', tipo);
    renderizarContenedoresMonitoreo(estado.contenedores, recepcion ? 'monitoreoContenedoresRecepcion' : 'monitoreoContenedoresSurtido');
    renderizarMovimientosMonitoreo(estado.movimientos, recepcion ? 'monitoreoMovimientosRecepcion' : 'monitoreoMovimientosSurtido');
}

// Función para cargar el detalle de monitoreo
async function cargarDetalleMonitoreo() {
    // Evitar múltiples solicitudes simultáneas
    if (monitoreoDetalleEnProceso) return;
    // Obtener el elemento principal que contiene los datos de la hoja y el tipo de monitoreo
    const elementoPrincipal = document.querySelector('main[data-id-hoja]');
    if (!elementoPrincipal) return;
    // Obtener el ID de la hoja y el tipo de monitoreo desde los atributos data del elemento principal
    const idHoja = elementoPrincipal.dataset.idHoja;
    const tipo = elementoPrincipal.dataset.tipoMonitoreo;
    if (!idHoja || !tipo) return;
    // Indicar que se está procesando la solicitud de detalle
    monitoreoDetalleEnProceso = true;
    // Realizar la solicitud para obtener el detalle de monitoreo según el tipo (recepción o surtido)
    try {
        const ruta = tipo === 'recepcion'
            ? `/monitoreo/api/recepciones/${encodeURIComponent(idHoja)}`
            : `/monitoreo/api/surtidos/${encodeURIComponent(idHoja)}`;
        const response = await fetch(ruta);
        const estado = await leerRespuestaMonitoreo(response, 'No fue posible consultar el detalle.');
        renderizarDetalleMonitoreo(estado, tipo);
    } catch (error) {
        if (error.message !== 'La sesión ha expirado.' && error.message !== 'Acceso denegado.' && typeof mostrarError === 'function') {
            mostrarError(error.message, 'No fue posible actualizar el detalle');
        }
    } finally {
        monitoreoDetalleEnProceso = false;
    }
}
// Función para iniciar el monitoreo, inicializando tablas y configurando intervalos de actualización
function iniciarMonitoreo() {
    // Inicializar las tablas de monitoreo si existen en el DOM
    inicializarTablasMonitoreo();
    // Determinar si estamos en el dashboard o en la vista de detalle según la presencia de elementos específicos
    const esDashboard = Boolean(document.getElementById('tablaMonitoreoSurtidos'));
    // Determinar si estamos en la vista de detalle según la presencia del atributo data-id-hoja en el elemento main
    const esDetalle = Boolean(document.querySelector('main[data-id-hoja]'));
    if (!esDashboard && !esDetalle) return;
    // Validar si estamos en el dashboard o en la vista de detalle, y asignar la función de carga correspondiente
    const cargar = esDashboard ? cargarDashboardMonitoreo : cargarDetalleMonitoreo;
    cargar();
    // Configurar un intervalo para actualizar los datos cada MONITOREO_INTERVALO_MS milisegundos
    monitoreoIntervalo = window.setInterval(cargar, MONITOREO_INTERVALO_MS);
    // Configurar el botón de actualización manual si existe en el DOM
    const botonActualizar = document.getElementById('btnActualizarMonitoreo');
    if (botonActualizar) botonActualizar.addEventListener('click', cargar);
}
// Iniciar el monitoreo cuando el DOM esté completamente cargado
document.addEventListener('DOMContentLoaded', iniciarMonitoreo);
