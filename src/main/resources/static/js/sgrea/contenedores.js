// Estados de contenedor
const ESTADO_CONTENEDOR_DISPONIBLE = 1;
const ESTADO_CONTENEDOR_EN_USO = 2;
const ESTADO_CONTENEDOR_MANTENIMIENTO = 3;
const ESTADO_CONTENEDOR_FUERA_DE_SERVICIO = 4;

let tablaContenedores;
let estadosContenedor = [];
let estadoActualContenedor = null;
// Función para escapar caracteres especiales en HTML y evitar inyección de código
function escaparHtmlContenedor(valor) {
    const caracteres = {'&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;'};
    return String(valor ?? '').replace(/[&<>"']/g, caracter => caracteres[caracter]);
}

// Función para obtener el nombre de un estado de contenedor
function obtenerNombreEstadoContenedor(idEstado, nombre = '') {
    const estado = estadosContenedor.find(item =>
        String(item.idEstadoContenedor) === String(idEstado));
    return estado?.nombre || nombre || 'Sin estado';
}
// Función para formatear el estado de un contenedor en la tabla
function formatearEstadoContenedor(cell) {
    const estado = cell.getValue() || {};
    const idEstado = Number(estado.idEstadoContenedor);
    const nombre = obtenerNombreEstadoContenedor(idEstado, estado.nombre);
    const clase = idEstado === ESTADO_CONTENEDOR_FUERA_DE_SERVICIO ? 'secondary'
        : idEstado === ESTADO_CONTENEDOR_MANTENIMIENTO ? 'warning'
            : idEstado === ESTADO_CONTENEDOR_EN_USO ? 'info' : 'success';
    return `<span class="badge text-bg-${clase}">${escaparHtmlContenedor(nombre)}</span>`;
}

// Función para formatear las acciones de un contenedor en la tabla
function formatearAccionesContenedor() {
    return '<button type="button" class="btn btn-sm btn-outline-danger" '
        + 'data-accion="eliminar" aria-label="Eliminar contenedor">'
        + '<i class="bi bi-trash"></i></button>';
}

// Función para formatear la fecha de un contenedor en la tabla
function formatearFechaContenedor(valor) {
    return valor ? new Date(valor).toLocaleString('es-MX') : '';
}

// Función para limpiar el formulario de contenedor
function limpiarFormularioContenedor() {
    const formulario = document.getElementById('formContenedor');
    formulario.reset();
    limpiarValidacionesFormulario(formulario);
    document.getElementById('idContenedor').value = '';
    document.getElementById('codigoContenedor').value = '';
    document.getElementById('fechaAltaContenedor').value = '';
    document.getElementById('capacidadContenedor').disabled = false;
    document.getElementById('estadoContenedor').disabled = true;
    document.getElementById('observacionesContenedor').disabled = false;
    estadoActualContenedor = null;
}

// Función para cargar los estados de contenedor desde el servidor
async function cargarEstadosContenedor() {
    const response = await fetch('/contenedor/estados');
    const data = await leerRespuestaContenedor(response);
    if (!response.ok) {
        throw new Error(data.mensaje || 'No fue posible cargar los estados de contenedor.');
    }
    estadosContenedor = data;
}

// Función para llenar el select de estados de contenedor
function llenarEstadosContenedor(idEstado, esNuevo) {
    const select = document.getElementById('estadoContenedor');
    select.innerHTML = '';
    // Definir los estados permitidos según si es nuevo o no
    const permitidos = esNuevo
        ? [ESTADO_CONTENEDOR_DISPONIBLE]
        : {
            [ESTADO_CONTENEDOR_DISPONIBLE]: [
                ESTADO_CONTENEDOR_DISPONIBLE,
                ESTADO_CONTENEDOR_MANTENIMIENTO,
                ESTADO_CONTENEDOR_FUERA_DE_SERVICIO
            ],
            [ESTADO_CONTENEDOR_EN_USO]: [ESTADO_CONTENEDOR_EN_USO],
            [ESTADO_CONTENEDOR_MANTENIMIENTO]: [
                ESTADO_CONTENEDOR_MANTENIMIENTO,
                ESTADO_CONTENEDOR_DISPONIBLE,
                ESTADO_CONTENEDOR_FUERA_DE_SERVICIO
            ],
            [ESTADO_CONTENEDOR_FUERA_DE_SERVICIO]: [ESTADO_CONTENEDOR_FUERA_DE_SERVICIO]
        }[Number(idEstado)] || [];
    // Llenar el select con los estados permitidos
    permitidos.forEach(id => {
        const estado = estadosContenedor.find(item => Number(item.idEstadoContenedor) === id);
        if (!estado) return;
        const option = document.createElement('option');
        option.value = estado.idEstadoContenedor;
        option.textContent = estado.nombre;
        select.appendChild(option);
    });
    // Establecer el valor seleccionado y deshabilitar el select si es necesario
    select.value = String(idEstado || ESTADO_CONTENEDOR_DISPONIBLE);
    select.disabled = esNuevo
        || Number(idEstado) === ESTADO_CONTENEDOR_EN_USO
        || Number(idEstado) === ESTADO_CONTENEDOR_FUERA_DE_SERVICIO;
}
// Función para abrir el modal de nuevo contenedor
async function nuevoContenedor() {
    try {
        // Limpiar el formulario y establecer el título del modal
        limpiarFormularioContenedor();
        document.getElementById('modalContenedorLabel').textContent = 'Nuevo contenedor';
        LoadingOverlay.mostrar('Cargando estados...');
        // Cargar los estados de contenedor y llenar el select con el estado disponible
        await cargarEstadosContenedor();
        llenarEstadosContenedor(ESTADO_CONTENEDOR_DISPONIBLE, true);
        // Mostrar el modal
        await LoadingOverlay.ocultar();
        bootstrap.Modal.getOrCreateInstance(document.getElementById('modalContenedor')).show();
    } catch (error) {
        await LoadingOverlay.ocultar();
        mostrarError(error.message, 'No fue posible cargar el formulario');
    }
}
// Función para abrir el modal de edición de contenedor
async function editarContenedor(contenedor) {
    try {
        // Limpiar el formulario y establecer el título del modal
        limpiarFormularioContenedor();
        document.getElementById('modalContenedorLabel').textContent = 'Editar contenedor';
        LoadingOverlay.mostrar('Cargando información...');
        // Cargar los estados de contenedor y llenar el select con el estado actual
        await cargarEstadosContenedor();
        // Establecer los valores del formulario con la información del contenedor
        const idEstado = Number(contenedor.estadoContenedor?.idEstadoContenedor);
        estadoActualContenedor = idEstado;
        document.getElementById('idContenedor').value = contenedor.idContenedor;
        document.getElementById('codigoContenedor').value = contenedor.codigo || '';
        document.getElementById('fechaAltaContenedor').value = formatearFechaContenedor(contenedor.fechaAlta);
        document.getElementById('capacidadContenedor').value = contenedor.capacidad ?? '';
        document.getElementById('observacionesContenedor').value = contenedor.observaciones || '';
        // Llenar el select de estados de contenedor
        llenarEstadosContenedor(idEstado, false);
        // Deshabilitar campos si el contenedor está en uso
        const bloqueado = idEstado === ESTADO_CONTENEDOR_EN_USO;
        document.getElementById('capacidadContenedor').disabled = bloqueado;
        document.getElementById('observacionesContenedor').disabled = bloqueado;
        // Mostrar el modal
        await LoadingOverlay.ocultar();
        bootstrap.Modal.getOrCreateInstance(document.getElementById('modalContenedor')).show();
    } catch (error) {
        await LoadingOverlay.ocultar();
        mostrarError(error.message, 'No fue posible cargar el contenedor');
    }
}
// Función para guardar o actualizar un contenedor
async function guardarContenedor() {
    const formulario = document.getElementById('formContenedor');
    // Validar el formulario antes de enviar los datos
    if (!formulario.checkValidity()) {
        formulario.classList.add('was-validated');
        return;
    }
    // Obtener el ID del contenedor y determinar si es nuevo o existente
    const idContenedor = document.getElementById('idContenedor').value;
    const esNuevo = !idContenedor;
    if (!esNuevo && estadoActualContenedor === ESTADO_CONTENEDOR_EN_USO) {
        await mostrarInformacion(
            'El contenedor en uso no puede modificarse desde el CRUD.',
            'Contenedor bloqueado');
        return;
    }
    // Confirmar la acción con el usuario antes de guardar o actualizar
    const confirmacion = await confirmarAccion({
        titulo: esNuevo ? '¿Guardar nuevo contenedor?' : '¿Actualizar contenedor?',
        mensaje: esNuevo
            ? 'Se registrará un nuevo contenedor reutilizable.'
            : 'Se actualizará el contenedor seleccionado.',
        textoConfirmar: esNuevo ? 'Sí, guardar' : 'Sí, actualizar',
        colorConfirmar: '#0d6efd'
    });
    if (!confirmacion.isConfirmed) return;

    // Construir el cuerpo de la solicitud
    const body = {
        capacidad: Number(document.getElementById('capacidadContenedor').value),
        observaciones: document.getElementById('observacionesContenedor').value
    };
    // Establecer la URL y el método HTTP según si es nuevo o existente
    const url = esNuevo
        ? '/contenedor/guardar'
        : '/contenedor/actualizar/' + encodeURIComponent(idContenedor);
    const method = esNuevo ? 'POST' : 'PUT';
    // Agregar el estado del contenedor solo si no es nuevo
    if (!esNuevo) {
        body.idEstadoContenedor = Number(document.getElementById('estadoContenedor').value);
    }

    LoadingOverlay.mostrar(esNuevo ? 'Guardando contenedor...' : 'Actualizando contenedor...');
    // Enviar la solicitud al servidor y manejar la respuesta
    try {
        const response = await fetch(url, {
            method,
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(body)
        });
        const data = await leerRespuestaContenedor(response);
        if (!response.ok) {
            throw new Error(data.mensaje || 'No fue posible guardar el contenedor.');
        }
        // Cerrar el modal y mostrar un mensaje de éxito
        await LoadingOverlay.ocultar();
        await mostrarExito(
            esNuevo ? 'Contenedor guardado correctamente.' : 'Contenedor actualizado correctamente.',
            'Operación exitosa');
        window.location.reload();
    } catch (error) {
        await LoadingOverlay.ocultar();
        mostrarError(error.message, 'No fue posible guardar');
    }
}
// Función para eliminar un contenedor
async function eliminarContenedor(idContenedor) {
    // Confirmar la acción con el usuario antes de eliminar
    const confirmacion = await confirmarAccion({
        titulo: '¿Deseas eliminar este contenedor?',
        mensaje: `Se eliminará el contenedor "${idContenedor}".`,
        textoConfirmar: 'Sí, eliminar',
        colorConfirmar: '#dc3545'
    });
    if (!confirmacion.isConfirmed) return;
    // Mostrar un overlay de carga mientras se realiza la eliminación
    LoadingOverlay.mostrar('Eliminando contenedor...');

    // Enviar la solicitud de eliminación al servidor y manejar la respuesta
    try {
        const response = await fetch('/contenedor/eliminar/' + encodeURIComponent(idContenedor), {
            method: 'DELETE'
        });
        const data = await leerRespuestaContenedor(response);
        if (!response.ok) {
            throw new Error(data.mensaje || 'No fue posible eliminar el contenedor.');
        }
        await LoadingOverlay.ocultar();
        await mostrarExito(data.mensaje, 'Contenedor eliminado');
        window.location.reload();
    } catch (error) {
        await LoadingOverlay.ocultar();
        mostrarError(error.message, 'No fue posible eliminar');
    }
}

// Función para leer la respuesta del servidor y manejar errores de parseo
async function leerRespuestaContenedor(response) {
    const texto = await response.text();
    if (!texto) return {};
    try {
        return JSON.parse(texto);
    } catch {
        return {mensaje: texto};
    }
}

// Función para inicializar la tabla de contenedores
function inicializarTablaContenedores() {
    // Obtener el contenedor de la tabla y verificar si Tabulator está disponible
    const contenedor = document.getElementById('tablaContenedores');
    if (!contenedor || typeof Tabulator === 'undefined') return;
    // Inicializar la tabla de contenedores con Tabulator
    tablaContenedores = new Tabulator(contenedor, {
        data: contenedoresIniciales ?? [],
        layout: 'fitDataStretch',
        responsiveLayout: false,
        pagination: true,
        paginationMode: 'local',
        paginationSize: 10,
        paginationSizeSelector: [10, 25, 50, 100],
        placeholder: 'No se encontraron contenedores.',
        // Configuración de las columnas de la tabla
        columnDefaults: {
            hozAlign: 'center',
            vertAlign: 'middle',
            headerHozAlign: 'center',
            cellClick: (event, cell) => {
                const elemento = event.target instanceof Element ? event.target : null;
                if (elemento?.closest('button')) return;
                editarContenedor(cell.getRow().getData());
            }
        },
        // Definición de las columnas de la tabla
        columns: [
            {title: 'Código', field: 'codigo', headerFilter: 'input',
                headerFilterPlaceholder: 'Buscar...', width: 160, minWidth: 160},
            {title: 'Capacidad', field: 'capacidad', headerFilter: 'input', width: 130, minWidth: 130},
            {title: 'Estado', field: 'estadoContenedor', formatter: formatearEstadoContenedor,
                width: 190, minWidth: 190},
            {title: 'Fecha de alta', field: 'fechaAlta', formatter: cell => formatearFechaContenedor(cell.getValue()),
                width: 190, minWidth: 190},
            {title: 'Observaciones', field: 'observaciones',
                formatter: cell => escaparHtmlContenedor(cell.getValue()),
                width: 300, minWidth: 300, hozAlign: 'left', headerHozAlign: 'left'},
            // Columna de acciones para borrar un contenedor
            {title: 'Borrar', formatter: formatearAccionesContenedor, headerSort: false,
                width: 110, minWidth: 110,
                cellClick: (event, cell) => {
                    if (event.target.closest('button')) {
                        eliminarContenedor(cell.getRow().getData().idContenedor);
                    }
                }}
        ],
        locale: 'es',
        langs: {es: {data: {loading: 'Cargando...', error: 'Error al cargar'}, pagination: {
            page_size: 'Filas por página', first: 'Primera', last: 'Última', prev: 'Anterior', next: 'Siguiente', all: 'Todos'
        }, headerFilters: {default: 'Filtrar columna...'}}}
    });
}
// Inicializar la tabla de contenedores cuando el DOM esté completamente cargado
document.addEventListener('DOMContentLoaded', inicializarTablaContenedores);
