// Estados estaticos de los conjuntos
const ESTADO_DISPONIBLE = 1;
const ESTADO_SURTIDO = 2;
const ESTADO_MANTENIMIENTO = 3;
const ESTADO_FUERA_DE_SERVICIO = 4;

// Variables globales
let tablaConjuntos;
let productosConjunto = [];
let estadosConjunto = [];
let tomSelectProductoConjunto = null;

// Función para escapar caracteres HTML en un valor dado
function escaparHtmlConjunto(valor) {
    const caracteres = {'&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;'};
    return String(valor ?? '').replace(/[&<>"']/g, caracter => caracteres[caracter]);
}
// Función para obtener el nombre del estado a partir de su ID
function obtenerNombreEstado(idEstado, nombre = '') {
    const estado = estadosConjunto.find(item => String(item.idEstadoConjunto) === String(idEstado));
    return estado?.nombre || nombre || 'Sin estado';
}
// Función para formatear el estado del conjunto en la tabla
function formatearEstadoConjunto(cell) {
    // Obtener el valor de la celda y determinar el nombre y la clase del estado
    const estado = cell.getValue() || {};
    const nombre = obtenerNombreEstado(estado.idEstadoConjunto, estado.nombre);
    // Determinar la clase del badge según el estado
    const clase = estado.idEstadoConjunto === ESTADO_FUERA_DE_SERVICIO ? 'secondary'
        : estado.idEstadoConjunto === ESTADO_MANTENIMIENTO ? 'warning'
            : estado.idEstadoConjunto === ESTADO_SURTIDO ? 'info' : 'success';
    // Devolver el HTML del badge con el nombre del estado
    return `<span class="badge text-bg-${clase}">${escaparHtmlConjunto(nombre)}</span>`;
}
// Función para formatear las acciones disponibles para cada conjunto en la tabla
function formatearAccionesConjunto() {
    return '<button type="button" class="btn btn-sm btn-outline-danger" data-accion="eliminar" aria-label="Eliminar conjunto">'
        + '<i class="bi bi-trash"></i></button>';
}
// Función para limpiar el formulario de conjunto y restablecer sus valores
function limpiarFormularioConjunto() {
    const formulario = document.getElementById('formConjunto');
    // Restablecer el formulario y limpiar las validaciones
    formulario.reset();
    // Eliminar la clase de validación y los mensajes de error
    limpiarValidacionesFormulario(formulario);
    // Limpiar los campos específicos del formulario
    document.getElementById('idConjunto').value = '';
    document.getElementById('idConjuntoVisible').value = '';
    document.getElementById('fechaAltaConjunto').value = '';
    // Limpiar y habilitar el select de productos
    if (tomSelectProductoConjunto) {
        tomSelectProductoConjunto.clear(true);
        tomSelectProductoConjunto.enable();
    } else {
        document.getElementById('productoConjunto').disabled = false;
    }
    document.getElementById('estadoConjunto').disabled = true;
}
// Función para cargar los catálogos de productos y estados de conjunto desde el servidor
async function cargarCatalogosConjunto() {
    // Realizar las solicitudes para obtener los productos y estados de conjunto
    const [productosResponse, estadosResponse] = await Promise.all([
        fetch('/conjunto/productos'),
        fetch('/conjunto/estados')
    ]);
    // Convertir las respuestas a JSON
    const productosData = await productosResponse.json();
    const estadosData = await estadosResponse.json();
    // Verificar si las respuestas fueron exitosas, de lo contrario lanzar un error
    if (!productosResponse.ok) {
        throw new Error(productosData.mensaje || 'No fue posible cargar los productos.');
    }
    if (!estadosResponse.ok) {
        throw new Error(estadosData.mensaje || 'No fue posible cargar los estados.');
    }
    // Asignar los datos obtenidos a las variables globales
    productosConjunto = productosData;
    estadosConjunto = estadosData;
}
// Función para llenar el select de productos en el formulario de conjunto
function llenarProductosConjunto(idProducto = '') {
    // Obtener el elemento select de productos y preparar las opciones
    const select = document.getElementById('productoConjunto');
    const opcionesProducto = productosConjunto.map(producto => ({
        value: String(producto.idProducto),
        text: producto.nombre
    }));
    // Determinar el ID del producto seleccionado, si se proporciona
    const idProductoSeleccionado = idProducto ? String(idProducto) : null;
    // Si TomSelect no está definido, llenar el select de manera tradicional
    if (typeof TomSelect === 'undefined') {
        select.innerHTML = '';
        // Agregar una opción por defecto al select según si hay productos disponibles o no
        select.appendChild(new Option(
            opcionesProducto.length ? 'Selecciona un producto' : 'No hay productos disponibles',
            ''
        ));
        // Agregar las opciones de productos al select
        opcionesProducto.forEach(opcion => select.add(new Option(opcion.text, opcion.value)));
        // Establecer el valor seleccionado en el select según el ID del producto proporcionado
        select.value = idProductoSeleccionado || '';
        return;
    }
    // Si TomSelect está definido, inicializarlo o actualizarlo según corresponda
    if (!tomSelectProductoConjunto) {
        tomSelectProductoConjunto = new TomSelect(select, {
            create: false,
            allowEmptyOption: true,
            placeholder: 'Seleccionar producto...',
            maxOptions: 500,
            sortField: [{field: 'text', direction: 'asc'}]
        });
    }
    // Limpiar las opciones existentes y agregar las nuevas opciones de productos
    tomSelectProductoConjunto.clear(true);
    tomSelectProductoConjunto.clearOptions();
    tomSelectProductoConjunto.addOption(opcionesProducto.length
        ? opcionesProducto
        : {value: '', text: 'No hay productos disponibles', disabled: true});
    // Actualizar las opciones del select y establecer el valor seleccionado según el ID del producto proporcionado
    tomSelectProductoConjunto.refreshOptions(false);
    if (idProductoSeleccionado) {
        tomSelectProductoConjunto.setValue(idProductoSeleccionado);
        tomSelectProductoConjunto.refreshItems();
    }
}
// Función para llenar el select de estados en el formulario de conjunto
function llenarEstadosConjunto(idEstado, esNuevo) {
    // Obtener el elemento select de estados y limpiar sus opciones
    const select = document.getElementById('estadoConjunto');
    select.innerHTML = '';
    // Determinar los estados permitidos según si es un nuevo conjunto o no
    const permitidos = esNuevo
        ? [ESTADO_DISPONIBLE]
        : {
            [ESTADO_DISPONIBLE]: [ESTADO_DISPONIBLE, ESTADO_MANTENIMIENTO, ESTADO_FUERA_DE_SERVICIO],
            [ESTADO_MANTENIMIENTO]: [ESTADO_MANTENIMIENTO, ESTADO_DISPONIBLE, ESTADO_FUERA_DE_SERVICIO],
            [ESTADO_SURTIDO]: [ESTADO_SURTIDO],
            [ESTADO_FUERA_DE_SERVICIO]: [ESTADO_FUERA_DE_SERVICIO]
        }[Number(idEstado)] || [];
    // Agregar las opciones de estados permitidos al select
    permitidos.forEach(id => {
        const estado = estadosConjunto.find(item => Number(item.idEstadoConjunto) === id);
        if (!estado) return;
        const option = document.createElement('option');
        option.value = estado.idEstadoConjunto;
        option.textContent = estado.nombre;
        select.appendChild(option);
    });
    // Establecer el valor seleccionado en el select según el idEstado proporcionado
    select.value = String(idEstado || ESTADO_DISPONIBLE);
    select.disabled = esNuevo || Number(idEstado) === ESTADO_SURTIDO || Number(idEstado) === ESTADO_FUERA_DE_SERVICIO;
}
// Función para abrir el modal de nuevo conjunto y preparar el formulario
async function nuevoConjunto() {
    try {
        // Verificar si hay un modal de conjunto abierto y cerrarlo antes de abrir uno nuevo
        limpiarFormularioConjunto();
        document.getElementById('modalConjuntoLabel').textContent = 'Nuevo conjunto';
        // Mostrar un overlay de carga mientras se cargan los catálogos y se llenan los selects
        LoadingOverlay.mostrar('Cargando productos...');
        // Cargar los catálogos de productos y estados de conjunto
        await cargarCatalogosConjunto();
        llenarProductosConjunto();
        llenarEstadosConjunto(ESTADO_DISPONIBLE, true);
        // Ocultar el overlay de carga y mostrar el modal de conjunto
        await LoadingOverlay.ocultar();
        bootstrap.Modal.getOrCreateInstance(document.getElementById('modalConjunto')).show();
    } catch (error) {
        await LoadingOverlay.ocultar();
        mostrarError(error.message, 'No fue posible cargar el formulario');
    }
}

// Función para abrir el modal de edición de conjunto y preparar el formulario
async function editarConjunto(conjunto) {
    try {
        // Limpiar el formulario y establecer el título del modal
        limpiarFormularioConjunto();
        document.getElementById('modalConjuntoLabel').textContent = 'Editar conjunto';
        LoadingOverlay.mostrar('Cargando información...');
        // Cargar los catálogos de productos y estados de conjunto
        await cargarCatalogosConjunto();
        // Llenar los campos del formulario con los datos del conjunto seleccionado
        document.getElementById('idConjunto').value = conjunto.idConjunto;
        document.getElementById('idConjuntoVisible').value = conjunto.idConjunto;
        // Llenar los selects de productos y estados con los valores correspondientes
        document.getElementById('fechaAltaConjunto').value = conjunto.fechaAlta
            ? new Date(conjunto.fechaAlta).toLocaleString('es-MX') : '';
        // Llenar el select de productos y deshabilitarlo para que no se pueda cambiar
        llenarProductosConjunto(conjunto.producto?.idProducto);
        tomSelectProductoConjunto?.disable();
        if (!tomSelectProductoConjunto) {
            document.getElementById('productoConjunto').disabled = true;
        }
        document.getElementById('observacionesConjunto').value = conjunto.observaciones || '';
        // Llenar el select de estados y deshabilitarlo si el estado es Surtido o Fuera de servicio
        const idEstado = conjunto.estadoConjunto?.idEstadoConjunto;
        llenarEstadosConjunto(idEstado, false);
        await LoadingOverlay.ocultar();
        // Mostrar el modal de conjunto para editar
        bootstrap.Modal.getOrCreateInstance(document.getElementById('modalConjunto')).show();
    } catch (error) {
        await LoadingOverlay.ocultar();
        mostrarError(error.message, 'No fue posible cargar el conjunto');
    }
}

// Función para guardar o actualizar un conjunto
async function guardarConjunto() {
    // Validar el formulario antes de enviar los datos
    const formulario = document.getElementById('formConjunto');
    if (!formulario.checkValidity()) {
        formulario.classList.add('was-validated');
        return;
    }
    // Obtener el ID del conjunto y determinar si es un nuevo conjunto o una actualización
    const idConjunto = document.getElementById('idConjunto').value;
    const esNuevo = !idConjunto;
    const confirmacion = await confirmarAccion({
        titulo: esNuevo ? '¿Guardar nuevo conjunto?' : '¿Actualizar conjunto?',
        mensaje: esNuevo ? 'Se registrará una nueva unidad física.' : 'Se actualizará el conjunto seleccionado.',
        textoConfirmar: esNuevo ? 'Sí, guardar' : 'Sí, actualizar',
        colorConfirmar: '#0d6efd'
    });
    // Si el usuario no confirma la acción, salir de la función
    if (!confirmacion.isConfirmed) return;
    // Obtener las observaciones del formulario y preparar el cuerpo de la solicitud según si es nuevo o no
    const observaciones = document.getElementById('observacionesConjunto').value;
    const body = esNuevo
        ? {idProducto: Number(document.getElementById('productoConjunto').value), observaciones}
        : {idConjunto, idEstadoConjunto: Number(document.getElementById('estadoConjunto').value), observaciones};
    // Mostrar un overlay de carga mientras se realiza la solicitud al servidor
    LoadingOverlay.mostrar(esNuevo ? 'Guardando conjunto...' : 'Actualizando conjunto...');
    try {
        // Realizar la solicitud al servidor para guardar o actualizar el conjunto
        const response = await fetch(esNuevo ? '/conjunto/guardar' : '/conjunto/actualizar', {
            method: esNuevo ? 'POST' : 'PUT',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(body)
        });
        // Obtener la respuesta en formato JSON y verificar si la solicitud fue exitosa
        const data = await response.json();
        if (!response.ok) throw new Error(data.mensaje || 'No fue posible guardar el conjunto.');
        await LoadingOverlay.ocultar();
        await mostrarExito(esNuevo ? 'Conjunto guardado correctamente.' : 'Conjunto actualizado correctamente.', 'Operación exitosa');
        window.location.reload();
    } catch (error) {
        // Ocultar el overlay de carga y mostrar un mensaje de error
        await LoadingOverlay.ocultar();
        mostrarError(error.message, 'No fue posible guardar');
    }
}
// Función para eliminar un conjunto
async function eliminarConjunto(idConjunto) {
    // Confirmar la acción de eliminar el conjunto
    const confirmacion = await confirmarAccion({
        titulo: '¿Deseas eliminar este conjunto?',
        mensaje: `Se eliminará el conjunto "${idConjunto}".`,
        textoConfirmar: 'Sí, eliminar',
        colorConfirmar: '#dc3545'
    });
    if (!confirmacion.isConfirmed) return;
    // Mostrar un overlay de carga mientras se realiza la solicitud de eliminación
    LoadingOverlay.mostrar('Eliminando conjunto...');
    try {
        // Realizar la solicitud al servidor para eliminar el conjunto
        const response = await fetch('/conjunto/eliminar/' + encodeURIComponent(idConjunto), {method: 'DELETE'});

        // Obtener la respuesta en formato JSON y verificar si la solicitud fue exitosa
        const data = await response.json();
        if (!response.ok) throw new Error(data.mensaje || 'No fue posible eliminar el conjunto.');
        await LoadingOverlay.ocultar();
        // Mostrar un mensaje de éxito y recargar la página
        await mostrarExito(data.mensaje, 'Conjunto eliminado');
        window.location.reload();
    } catch (error) {
        await LoadingOverlay.ocultar();
        // Mostrar un mensaje de error si la solicitud de eliminación falla
        mostrarError(error.message, 'No fue posible eliminar');
    }
}
// Función para inicializar la tabla de conjuntos utilizando Tabulator
function inicializarTablaConjuntos() {
    // Obtener el contenedor de la tabla y verificar si Tabulator está disponible
    const contenedor = document.getElementById('tablaConjuntos');
    if (!contenedor || typeof Tabulator === 'undefined') return;
    // Inicializar la tabla de conjuntos con las configuraciones necesarias
    tablaConjuntos = new Tabulator(contenedor, {
        data: conjuntosIniciales ?? [],
        layout: 'fitDataStretch',
        responsiveLayout: false,
        pagination: true,
        paginationMode: 'local',
        paginationSize: 10,
        paginationSizeSelector: [10, 25, 50, 100],
        placeholder: 'No se encontraron conjuntos.',
        // Configuración de los filtros de encabezado
        columnDefaults: {
            hozAlign: 'center',
            vertAlign: 'middle',
            headerHozAlign: 'center',
            cellClick: (event, cell) => {
                const elemento = event.target instanceof Element ? event.target : null;
                if (elemento?.closest('button')) return;
                editarConjunto(cell.getRow().getData());
            }
        },
        // Definición de las columnas de la tabla
        columns: [
            {title: 'Identificador', field: 'idConjunto', headerFilter: 'input', headerFilterPlaceholder: 'Buscar...', width: 150, minWidth: 150},
            {title: 'Producto', field: 'producto.nombre', headerFilter: 'input', headerFilterPlaceholder: 'Buscar producto...', width: 220, minWidth: 220, hozAlign: 'left', headerHozAlign: 'left'},
            {title: 'Estado', field: 'estadoConjunto', formatter: formatearEstadoConjunto, width: 180, minWidth: 180},
            {title: 'Fecha de alta', field: 'fechaAlta', formatter: cell => cell.getValue() ? new Date(cell.getValue()).toLocaleString('es-MX') : '', width: 190, minWidth: 190},
            {title: 'Observaciones', field: 'observaciones', formatter: cell => escaparHtmlConjunto(cell.getValue()), width: 300, minWidth: 300, hozAlign: 'left', headerHozAlign: 'left'},
            {title: 'Borrar', formatter: formatearAccionesConjunto, headerSort: false, width: 110, minWidth: 110,
                cellClick: (event, cell) => {
                    if (event.target.closest('button')) eliminarConjunto(cell.getRow().getData().idConjunto);
                }}
        ],
        locale: 'es',
        langs: {es: {data: {loading: 'Cargando...', error: 'Error al cargar'}, pagination: {
            page_size: 'Filas por página', first: 'Primera', last: 'Última', prev: 'Anterior', next: 'Siguiente', all: 'Todos'
        }, headerFilters: {default: 'Filtrar columna...'}}}
    });
}
// Inicializar la tabla de conjuntos cuando el DOM esté completamente cargado
document.addEventListener('DOMContentLoaded', inicializarTablaConjuntos);
