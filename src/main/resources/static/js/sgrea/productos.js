// Función para vaciar el formulario
let urlImagenProductoActual = null;

function vaciarFormulario(){
    // Obtenemos el formulario y lo reseteamos
    const formulario = document.getElementById("formProducto");
    formulario.reset();
    limpiarValidacionesFormulario(formulario);
    // Limpiamos el campo oculto del idProducto y la vista previa de la imagen
    document.getElementById("idProducto").value = "";
    document.getElementById("imagen").setCustomValidity('');
    urlImagenProductoActual = null;
    // Limpiamos la vista previa de la imagen
    mostrarVistaPreviaImagen(null, true);
}

const imagenProductoPredeterminada = '/img/img-404.png';

// Función para mostrar la vista previa de la imagen
function mostrarVistaPreviaImagen(urlImagen, usarImagenPredeterminada = false) {
    // Obtenemos el contenedor y la imagen de vista previa
    const contenedor = document.getElementById("vistaPreviaImagen");
    const imagen = document.getElementById("imagenProductoPreview");
    const imagenAMostrar = urlImagen || (usarImagenPredeterminada ? imagenProductoPredeterminada : null);

    // Si hay una URL de imagen, la mostramos y quitamos la clase d-none del contenedor
    if (imagenAMostrar) {
        imagen.onerror = () => {
            imagen.onerror = null;
            imagen.src = imagenProductoPredeterminada;
        };
        imagen.src = imagenAMostrar;
        contenedor.classList.remove("d-none");
        return;
    }

    // Si no hay URL de imagen, quitamos el src de la imagen y agregamos la clase d-none al contenedor
    imagen.onerror = null;
    imagen.removeAttribute("src");
    contenedor.classList.add("d-none");
}

// Listener al input de imagen para mostrar la vista previa cuando se seleccione un archivo
document.getElementById("imagen").addEventListener("change", event => {
    const archivo = event.target.files[0];
    const campoImagen = event.target;
    campoImagen.setCustomValidity('');
    mostrarVistaPreviaImagen(archivo ? URL.createObjectURL(archivo) : null, true);
});

// Función para llenar el select de bodegas según el idUsuario
async function llenarBodegaSesion(idUsuario, idBodegaSesion) {
    LoadingOverlay.mostrar('Cargando bodegas...');
    // Limpiar el select de bodegas
    const bodegaSelect = document.getElementById("bodega");
    bodegaSelect.innerHTML = "";

    // Obtener las bodegas del usuario
    try {
        const response = await fetch('/productos/getBodegas/' + idUsuario);
        if (!response.ok) throw new Error('No fue posible cargar las bodegas.');
        const bodegas = await response.json();
        // Buscar la bodega de la sesión en la lista de bodegas del usuario
        const bodegaSesion = bodegas.find(bodega => String(bodega.idBodega) === String(idBodegaSesion));
        if (!bodegaSesion) {
            throw new Error('La bodega de la sesión no está autorizada para el usuario.');
        }
        // Agregar la bodega de la sesión al select y deshabilitarlo
        const option = document.createElement("option");
        option.value = bodegaSesion.idBodega;
        option.textContent = bodegaSesion.nombre;
        bodegaSelect.appendChild(option);
        bodegaSelect.value = String(bodegaSesion.idBodega);
        bodegaSelect.disabled = true;
    } finally {
        await LoadingOverlay.ocultar();
    }
}

// Función para mostrar el modal de producto
function mostrarModalProducto() {
    const modalElement = document.getElementById("modalProducto");
    bootstrap.Modal.getOrCreateInstance(modalElement).show();
}

// Función para mostrar el modal de producto
async function nuevoProducto(btn) {
    // Obtener el idUsuario del botón
    const idUsuario = btn.dataset.idUsuario;
    const idBodegaSesion = btn.dataset.idBodega;
    // Vaciar el formulario y cambiar el título del modal
    vaciarFormulario();
    document.getElementById("modalProductoLabel").textContent = "Nuevo producto";

    // Llenar el select de bodegas y mostrar el modal
    try {
        await llenarBodegaSesion(idUsuario, idBodegaSesion);
        mostrarModalProducto();
    } catch (error) {
        console.error('Error al cargar las bodegas:', error);
        mostrarError('No fue posible cargar las bodegas.');
    }
}

// Función para mostrar el modal de producto con los datos del producto a editar
async function editarProducto(idUsuario, idProducto) {
    // Vaciar el formulario y cambiar el título del modal
    vaciarFormulario();
    document.getElementById("modalProductoLabel").textContent = "Editar producto";
    const idBodegaSesion = document.getElementById("tablaProductos")?.dataset.idBodega;

    // Llenar el select de bodegas y obtener la información del producto
    try {
        LoadingOverlay.mostrar('Cargando producto...');
        const productoRequest = fetch('/productos/getProducto/' + idProducto)
            .then(response => {
                if (!response.ok) {
                    throw new Error('No fue posible cargar el producto.');
                }
                return response.json();
            });

        // Usar Promise.all para esperar a que ambas promesas se resuelvan
        const [, producto] = await Promise.all([
            llenarBodegaSesion(idUsuario, idBodegaSesion),
            productoRequest
        ]);

        if (!producto) {
            throw new Error('El producto no existe.');
        }

        // Llenar el formulario con los datos del producto
        document.getElementById("idProducto").value = producto.idProducto;
        document.getElementById("nombre").value = producto.nombre;
        document.getElementById("descripcion").value = producto.descripcion;
        document.getElementById("bodega").value = String(idBodegaSesion);
        document.getElementById("bodega").disabled = true;
        document.getElementById("activo").checked = producto.activo;
        urlImagenProductoActual = producto.urlImagen || null;
        mostrarVistaPreviaImagen(producto.urlImagen, true);
        mostrarModalProducto();
        await LoadingOverlay.ocultar();
    } catch (error) {
        LoadingOverlay.ocultar();
        console.error('Error al cargar el producto:', error);
        mostrarError('No fue posible cargar la información del producto.');
    }
}

// Función para guardar el producto (nuevo o editado)
async function guardarProducto() {
    const formulario = document.getElementById("formProducto");
    const campoImagen = document.getElementById("imagen");
    const archivoImagen = campoImagen.files[0];

    // Una imagen existente en BD o un archivo nuevo son válidos para guardar.
    campoImagen.setCustomValidity(archivoImagen || urlImagenProductoActual
        ? ''
        : 'Selecciona una imagen para el producto.');

    // Validar el formulario y mostrar los mensajes Bootstrap como tooltip.
    if (!formulario.checkValidity()) {
        formulario.classList.add("was-validated");
        return;
    }

    const esNuevo = !document.getElementById("idProducto").value;
    const confirmacion = await confirmarAccion({
        titulo: esNuevo ? '¿Guardar nuevo producto?' : '¿Actualizar producto?',
        mensaje: esNuevo
            ? 'Se creará el producto con la información capturada.'
            : 'Se actualizará la información del producto.',
        textoConfirmar: esNuevo ? 'Sí, guardar' : 'Sí, actualizar',
        colorConfirmar: '#0d6efd'
    });

    if (!confirmacion.isConfirmed) {
        return;
    }

    // Obtener los datos del formulario
    const idProducto = document.getElementById("idProducto").value;
    const nombre = document.getElementById("nombre").value;
    const descripcion = document.getElementById("descripcion").value;
    const bodegaId = document.getElementById("bodega").value;
    const activo = document.getElementById("activo").checked;

    // Crear objeto con los datos del producto
    const producto = {
        idProducto: idProducto,
        nombre: nombre,
        descripcion: descripcion,
        bodega: {
            idBodega: bodegaId
        },
        activo: activo
    };

    // Crear FormData para enviar el producto y la imagen al servidor
    const datosFormulario = new FormData();
    datosFormulario.append("producto", new Blob([JSON.stringify(producto)], {
        type: "application/json"
    }));

    // Si hay un archivo de imagen, agregarlo al FormData
    if (archivoImagen) {
        datosFormulario.append("imagen", archivoImagen);
    }

    // Enviar los datos al servidor usando fetch
    LoadingOverlay.mostrar('Guardando producto...');
    fetch('/productos/guardar', {
        method: 'POST',
        body: datosFormulario
    })
    .then(async response => {
        const data = await response.json();
        if (!response.ok) {
            throw new Error(data.mensaje || 'No fue posible guardar el producto.');
        }
        return data;
    })
    .then(async data => {
        // Manejar la respuesta del servidor
        await LoadingOverlay.ocultar();
        await mostrarExito(data.mensaje, 'Producto guardado');

        // Volver a cargar la página para reflejar los cambios
        window.location.reload();
    })
    .catch(async error => {
        console.error('Error:', error);
        await LoadingOverlay.ocultar();
        mostrarError(error.message, 'No fue posible guardar');
    });
}

// Función para eliminar un producto
async function eliminarProducto(idProducto, nombreProducto) {
    const resultado = await confirmarAccion({
        titulo: '¿Deseas eliminar este producto?',
        mensaje: `¿Deseas eliminar el producto "${nombreProducto || 'Sin nombre'}"?`,
        textoConfirmar: 'Sí, eliminar',
        colorConfirmar: '#dc3545'
    });

    if (!resultado.isConfirmed) {
        return;
    }

    LoadingOverlay.mostrar('Eliminando producto...');
    fetch('/productos/eliminar/' + idProducto, {
        method: 'DELETE'
    })
    .then(async response => {
        const data = await response.json();
        if (!response.ok) {
            throw new Error(data.mensaje || 'No fue posible eliminar el producto.');
        }
        return data;
    })
    .then(async data => {
        await LoadingOverlay.ocultar();
        await mostrarExito(data.mensaje, 'Producto eliminado');
        window.location.reload();
    })
    .catch(async error => {
        console.error('Error:', error);
        await LoadingOverlay.ocultar();
        mostrarError(error.message, 'No fue posible eliminar');
    });
}

// Función para escapar caracteres HTML y prevenir inyección de código
function escaparHtml(valor) {
    const caracteres = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;'
    };

    return String(valor ?? '').replace(/[&<>"']/g, caracter => caracteres[caracter]);
}

// Función para mostrar la miniatura del producto en la tabla
function formatearImagen(cell) {
    const producto = cell.getRow().getData();
    const imagenPredeterminada = '/img/img-404.png';
    const urlImagen = cell.getValue() || imagenPredeterminada;

    return `<img src="${escaparHtml(urlImagen)}"
                 alt="${escaparHtml(`Imagen de ${producto.nombre}`)}"
                 class="img-thumbnail"
                 style="width: 84px; height: 84px; object-fit: cover; display: block; margin: 0 auto;"
                 onerror="this.onerror=null; this.src='${imagenPredeterminada}';">`;
}

// Función para formatear la columna de descripción
function formatearDescripcion(cell) {
    return `<span class="text-muted">${escaparHtml(cell.getValue())}</span>`;
}

// Función para formatear la columna de nombre del producto
function formatearNombreProducto(cell) {
    return `<strong>${escaparHtml(cell.getValue())}</strong>`;
}

// Función para formatear la columna de estado
function formatearEstado(cell) {
    const activo = cell.getValue() === true;
    const clase = activo ? 'success' : 'danger';
    const texto = activo ? 'Activo' : 'Inactivo';
    return `<span class="badge text-bg-${clase}">${texto}</span>`;
}

// Función para formatear la columna de acciones
function formatearAcciones() {
    return `<button type="button" class="btn btn-sm btn-outline-danger" data-accion="eliminar" aria-label="Eliminar producto">
                <i class="bi bi-trash"></i>
            </button>`;
}

// Función para manejar las acciones de los botones en la columna de acciones
function manejarAccionProducto(event, cell) {
    const boton = event.target.closest('button[data-accion]');
    if (!boton) {
        return;
    }

    const producto = cell.getRow().getData();
    const idUsuario = document.getElementById('tablaProductos').dataset.idUsuario;

    // Dependiendo de la acción del botón, llamar a la función correspondiente
    if (boton.dataset.accion === 'editar') {
        editarProducto(idUsuario, producto.idProducto);
        return;
    }

    eliminarProducto(producto.idProducto, producto.nombre);
}

// Función para inicializar la tabla de productos usando Tabulator
function inicializarTablaProductos() {
    const contenedor = document.getElementById('tablaProductos');

    if (!contenedor || typeof Tabulator === 'undefined') {
        console.error('No fue posible inicializar la tabla de productos.');
        return;
    }

    // Obtener los productos iniciales desde el atributo data-productos del contenedor
    const tablaProductos = new Tabulator(contenedor, {
        data: productosIniciales ?? [],
        layout: 'fitColumns',
        responsiveLayout: 'collapse',
        resizableColumns: false,
        pagination: true, // Habilitar la paginación
        paginationMode: 'local', // Usar paginación local para manejar los datos en el cliente
        paginationSize: 10, // Número de filas por página
        paginationSizeSelector: [10, 25, 50, 100], // Opciones de tamaño de página
        placeholder: 'No se encontraron productos.', // Mensaje cuando no hay datos
        columnDefaults: {
            hozAlign: 'center',
            vertAlign: 'middle',
            headerHozAlign: 'center',
            cellClick: (event, cell) => {
                const elemento = event.target instanceof Element ? event.target : null;
                if (elemento?.closest('button')) {
                    return;
                }

                const producto = cell.getRow().getData();
                editarProducto(contenedor.dataset.idUsuario, producto.idProducto);
            }
        },
        columns: [
            { title: 'ID', field: 'idProducto', width: 80 }, // Columna para el ID del producto
            {
                title: 'Imagen',
                field: 'urlImagen',
                formatter: formatearImagen,
                headerSort: false,
                variableHeight: false,
                width: 110
            },
            {
                title: 'Producto',
                field: 'nombre',
                headerFilter: 'input',
                headerFilterPlaceholder: 'Buscar producto...',
                cssClass: 'text-wrap',
                variableHeight: false,
                formatter: formatearNombreProducto,
                minWidth: 80,
                hozAlign: 'left',
                headerHozAlign: 'left'
            },
            {
                title: 'Descripción',
                field: 'descripcion',
                headerFilter: false,
                cssClass: 'text-wrap',
                variableHeight: false,
                formatter: formatearDescripcion,
                minWidth: 320,
                hozAlign: 'left',
                headerHozAlign: 'left'
            },
            {
                title: 'Bodega',
                field: 'bodega.nombre',
                headerSort: false,
                variableHeight: false,
                width: 170,
                minWidth: 150
            },
            {
                title: 'Estado',
                field: 'activo',
                formatter: formatearEstado,
                headerFilter: 'list',
                variableHeight: false,
                headerFilterParams: {
                    values: {
                        '': 'Ambos...',
                        true: 'Activo',
                        false: 'Inactivo'
                    }
                },
                headerFilterFunc: (valorFiltro, valorFila) =>
                    valorFiltro === '' || String(valorFila) === valorFiltro,
                width: 140
            },
            {
                title: 'Borrar',
                formatter: formatearAcciones,
                headerSort: false,
                cellClick: manejarAccionProducto,
                variableHeight: false,
                width: 125
            }
        ],
        locale: "es",
        langs: {
            es: {
                data: {
                    loading: "Cargando...",
                    error: "Error al cargar"
                },
                pagination: {
                    page_size: "Filas por página",
                    page_title: "Mostrar página",
                    first: "Primera",
                    first_title: "Primera página",
                    last: "Última",
                    last_title: "Última página",
                    prev: "Anterior",
                    prev_title: "Página anterior",
                    next: "Siguiente",
                    next_title: "Página siguiente",
                    all: "Todos",
                    counter: {
                        showing: "Mostrando",
                        of: "de",
                        rows: "filas",
                        pages: "páginas"
                    }
                },
                headerFilters: {
                    default: "Filtrar columna..."
                }
            }
        }
    });

}

// Inicializar la tabla de productos cuando el DOM esté completamente cargado
document.addEventListener('DOMContentLoaded', inicializarTablaProductos);
