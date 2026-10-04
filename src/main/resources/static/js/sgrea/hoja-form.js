document.addEventListener("DOMContentLoaded", () => {
    const contenedorVista = document.querySelector(".app-main");
    const formularioHoja = document.getElementById("formHoja");
    const botonBloquearHoja = document.getElementById("btnBloquearHoja");
    const botonEliminarHoja = document.getElementById("btnEliminarHoja");
    const formularioDetalle = document.getElementById("formDetalle");
    const botonAgregarDetalle = document.getElementById("btnAgregarDetalle");
    const modalDetalle = document.getElementById("modalDetalle");
    const modalDetalleTitulo = document.getElementById("modalDetalleLabel");
    const idHojaInput = document.getElementById("idHoja");
    const idDetalleInput = document.getElementById("idDetalle");
    const idHojaDetalleInput = document.getElementById("idHojaDetalle");
    const productoSelect = document.getElementById("producto");
    const cantidadSolicitadaInput = document.getElementById("cantidadSolicitada");
    const tablaDetalles = document.getElementById("tablaDetallesHoja");
    const resumenDetalles = document.getElementById("detalleHojaResumen");
    const totalProductos = document.getElementById("detalleHojaTotalProductos");
    const totalPiezas = document.getElementById("detalleHojaTotalPiezas");
    const soloLectura = contenedorVista?.dataset.soloLectura === "true";

    // Configuración de Flatpickr para los campos de fecha y hora
    const opcionesFechaHora = {
        enableTime: true,
        time_24hr: true,
        dateFormat: "Y-m-d\\TH:i",
        altInput: true,
        altFormat: "d/m/Y H:i",
        locale: typeof flatpickr !== "undefined" && flatpickr.l10ns?.es
            ? flatpickr.l10ns.es
            : "default",
        allowInput: false,
        clickOpens: !soloLectura
    };
    // Inicializar Flatpickr solo si está disponible
    if (typeof flatpickr !== "undefined") {
        flatpickr("#fechaSalida", opcionesFechaHora);
        flatpickr("#fechaRegreso", opcionesFechaHora);
    }
    // Obtener el ID de la hoja de producción desde el input oculto
    const idHoja = idHojaInput?.value;
    // Inicializar el modal de detalle usando Bootstrap
    const modalDetalleInstance = modalDetalle ? bootstrap.Modal.getOrCreateInstance(modalDetalle) : null;
    let tomSelectProducto = null;
    let tablaDetallesInstancia = null;
    let modoDetalle = "crear";

    // Mostrar alerta inicial si existe un mensaje en el contenedor principal
    mostrarAlertaCargaInicial(contenedorVista);

    // Si el formulario de la hoja existe, agregar un listener para la validación al enviar
    if (formularioHoja) {
        formularioHoja.addEventListener("submit", async event => {
            const fechaSalida = document.getElementById("fechaSalida");
            const contenedorFechaSalida = fechaSalida?.closest(".position-relative");
            const tooltipFechaSalida = contenedorFechaSalida?.querySelector(".invalid-tooltip");
            const fechaSalidaValida = Boolean(fechaSalida?.value);

            fechaSalida?.setCustomValidity(fechaSalidaValida ? "" : "Captura la fecha de salida.");

            // Flatpickr crea un input visual alternativo; también se marca ese campo.
            const fechaSalidaVisible = fechaSalida?._flatpickr?.altInput || fechaSalida;
            fechaSalidaVisible?.classList.toggle("is-invalid", !fechaSalidaValida);
            tooltipFechaSalida?.classList.toggle("d-block", !fechaSalidaValida);
            // Si el formulario no es válido, prevenir el envío y detener la propagación del evento
            if (!formularioHoja.checkValidity()) {
                event.preventDefault();
                event.stopPropagation();
            }
            // Agregar la clase "was-validated" para mostrar los estilos de validación
            formularioHoja.classList.add("was-validated");

            // Las hojas existentes requieren confirmación antes de actualizarse.
            if (formularioHoja.checkValidity() && idHoja) {
                event.preventDefault();
                const nombreProyecto = document.getElementById("nombreProyecto")?.value?.trim() || "Sin nombre";
                const confirmacion = await confirmarAccion({
                    titulo: "¿Actualizar hoja de producción?",
                    mensaje: `¿Deseas actualizar la información de la hoja "${nombreProyecto}"?`,
                    textoConfirmar: "Sí, actualizar",
                    colorConfirmar: "#0d6efd"
                });

                if (confirmacion.isConfirmed) {
                    HTMLFormElement.prototype.submit.call(formularioHoja);
                }
            }
        });
    }

    // Agregar listener para el botón de eliminar hoja
    botonBloquearHoja?.addEventListener("click", async () => {
        if (!idHoja) return;
        // Mostrar un cuadro de confirmación antes de bloquear la hoja
        const confirmacion = await confirmarAccion({
            titulo: "¿Bloquear hoja para surtido?",
            mensaje: "La hoja y sus detalles dejarán de ser editables.",
            textoConfirmar: "Sí, bloquear hoja",
            colorConfirmar: "#ffc107"
        });

        if (!confirmacion.isConfirmed) return;
        // Intentar bloquear la hoja de producción mediante una solicitud POST
        try {
            LoadingOverlay.mostrar("Bloqueando hoja...");
            const response = await fetch(`/hojas-produccion/${idHoja}/bloquear`, {method: "POST"});
            // Leer la respuesta JSON y manejar el caso de sesión expirada (401)
            const data = await leerRespuestaJson(response);
            if (!response.ok) {
                throw new Error(data.mensaje || "No fue posible bloquear la hoja.");
            }
            // Si la respuesta es exitosa, ocultar el overlay de carga, mostrar un mensaje de éxito y recargar la página
            await LoadingOverlay.ocultar();
            await mostrarExito(data.mensaje || "La hoja quedó lista para surtido.", "Hoja bloqueada");
            window.location.reload();
        } catch (error) {
            await LoadingOverlay.ocultar();
            manejarErrorFetch(error, "No fue posible bloquear la hoja.");
        }
    });

    // Agregar listener para el botón de eliminar hoja
    botonEliminarHoja?.addEventListener("click", async () => {
        if (!idHoja) {
            return;
        }
        // Mostrar un cuadro de confirmación antes de eliminar la hoja
        const confirmacion = await confirmarAccion({
            titulo: "¿Eliminar hoja de producción?",
            mensaje: "Esta acción eliminará también sus detalles.",
            textoConfirmar: "Sí, eliminar",
            colorConfirmar: "#dc3545"
        });
        if (!confirmacion.isConfirmed) {
            return;
        }
        // Intentar eliminar la hoja de producción mediante una solicitud DELETE
        try {
            const response = await fetch(`/hojas-produccion/${idHoja}`, {
                method: "DELETE"
            });
            const data = await leerRespuestaJson(response);
            // Si la respuesta no es exitosa, lanzar un error con el mensaje recibido o un mensaje por defecto
            if (!response.ok) {
                throw new Error(data.mensaje || "No fue posible eliminar la hoja de produccion.");
            }
            // Mostrar un mensaje de éxito y redirigir a la lista de hojas de producción
            mostrarAlerta("success", data.mensaje || "Hoja de produccion eliminada correctamente.");
            window.location.href = "/hojas-produccion";
        } catch (error) {
            manejarErrorFetch(error, "No fue posible eliminar la hoja de produccion.");
        }
    });

    // Si hay un ID de hoja y el cuerpo de la tabla de detalles existe, cargar los detalles de la hoja
    if (idHoja && tablaDetalles) {
        cargarDetallesHoja();
    }
    // Si no es solo lectura y existen los elementos necesarios, agregar listeners para el modal de detalle y el formulario de detalle
    if (!soloLectura && modalDetalle && formularioDetalle && productoSelect) {
        modalDetalle.addEventListener("hidden.bs.modal", () => {
            modoDetalle = "crear";
            limpiarFormularioDetalle();
        });
        // Agregar listener para el botón de agregar detalle
        botonAgregarDetalle?.addEventListener("click", async () => {
            try {
                modoDetalle = "crear";
                await inicializarSelectProductos();
                limpiarFormularioDetalle();
                modalDetalleInstance.show();
            } catch (error) {
                console.error("Error al cargar los productos:", error);
                mostrarAlerta("error", "No fue posible cargar los productos de la bodega.");
            }
        });

        // Agregar listener para el envío del formulario de detalle
        formularioDetalle.addEventListener("submit", async event => {
            event.preventDefault();
            // Si el formulario no es válido, detener la propagación del evento y agregar la clase "was-validated"
            if (!formularioDetalle.checkValidity()) {
                event.stopPropagation();
                formularioDetalle.classList.add("was-validated");
                return;
            }
            // Construir el objeto detalle a enviar al servidor
            const detalle = {
                idDetalleHoja: idDetalleInput.value ? Number(idDetalleInput.value) : null,
                hojaProduccion: {
                    idHoja: Number(idHojaDetalleInput.value)
                },
                producto: {
                    idProducto: Number(productoSelect.value)
                },
                cantidadSolicitada: Number(cantidadSolicitadaInput.value)
            };

            // Intentar enviar el detalle al servidor mediante una solicitud POST
            try {
                const response = await fetch("/hojas-produccion/detalle/guardar", {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify(detalle)
                });
                const data = await leerRespuestaJson(response);

                if (!response.ok) {
                    throw new Error(data.mensaje || "No fue posible guardar el detalle.");
                }
                // Si la respuesta es exitosa, limpiar el formulario, ocultar el modal y recargar los detalles de la hoja
                limpiarValidacionesFormulario(formularioDetalle);
                modalDetalleInstance.hide();
                await cargarDetallesHoja();
                mostrarAlerta("success", data.mensaje || "Detalle guardado correctamente.");
            } catch (error) {
                manejarErrorFetch(error, "No fue posible guardar el detalle.");
            }
        });
    }

    // Cargar los detalles de la hoja de producción
    async function cargarDetallesHoja() {
        try {
            const response = await fetch(`/hojas-produccion/${idHoja}/detalles`);
            const detalles = await leerRespuestaJson(response);

            if (!response.ok) {
                throw new Error("No fue posible obtener los detalles de la hoja.");
            }

            renderizarTablaDetalles(Array.isArray(detalles) ? detalles : []);
        } catch (error) {
            manejarErrorFetch(error, "No fue posible cargar los detalles de la hoja.");
        }
    }

    // Abrir el modal de edición de un detalle
    async function abrirEdicionDetalle(idDetalle) {
        try {
            const response = await fetch(`/hojas-produccion/detalle/${idDetalle}`);
            const detalle = await leerRespuestaJson(response);

            if (!response.ok) {
                throw new Error("No fue posible obtener la informacion del detalle.");
            }
            // Inicializar el select de productos con el producto del detalle
            await inicializarSelectProductos(detalle.producto);
            // Configurar el modo de detalle como "editar" y llenar los campos del formulario con la información del detalle
            modoDetalle = "editar";
            idDetalleInput.value = detalle.idDetalleHoja || "";
            idHojaDetalleInput.value = detalle.hojaProduccion?.idHoja || idHoja;
            cantidadSolicitadaInput.value = detalle.cantidadSolicitada ?? "";

            modalDetalleTitulo.textContent = "Editar detalle";
            limpiarValidacionesFormulario(formularioDetalle);
            modalDetalleInstance.show();
        } catch (error) {
            manejarErrorFetch(error, "No fue posible cargar la informacion del detalle.");
        }
    }

    // Eliminar un detalle
    async function eliminarDetalle(idDetalle, nombreProducto) {
        const confirmacion = await confirmarAccion({
            titulo: "¿Deseas eliminar este detalle?",
            mensaje: `¿Deseas eliminar el detalle con el producto "${nombreProducto || "Sin nombre"}"?`,
            textoConfirmar: "Sí, eliminar",
            colorConfirmar: "#dc3545"
        });
        if (!confirmacion.isConfirmed) {
            return;
        }
        // Intentar eliminar el detalle mediante una solicitud DELETE
        try {
            const response = await fetch(`/hojas-produccion/detalle/${idDetalle}`, {
                method: "DELETE"
            });
            const data = await leerRespuestaJson(response);

            if (!response.ok) {
                throw new Error(data.mensaje || "No fue posible eliminar el detalle.");
            }
            // Si la respuesta es exitosa, recargar los detalles de la hoja y mostrar un mensaje de éxito
            await cargarDetallesHoja();
            mostrarAlerta("success", data.mensaje || "Detalle eliminado correctamente.");
        } catch (error) {
            manejarErrorFetch(error, "No fue posible eliminar el detalle.");
        }
    }

    // Inicializar el select de productos usando TomSelect y cargar los productos desde la bodega
    async function inicializarSelectProductos(productoSeleccionado = null) {
        const productos = await cargarProductosBodega();
        const idProductoSeleccionado = productoSeleccionado?.idProducto != null
            ? String(productoSeleccionado.idProducto)
            : null;
        const opcionesProducto = productos.map(producto => ({
            value: String(producto.idProducto),
            text: producto.nombre
        }));

        // Si hay un producto seleccionado que no está en la lista de opciones, agregarlo
        if (idProductoSeleccionado && !opcionesProducto.some(opcion => opcion.value === idProductoSeleccionado)) {
            opcionesProducto.push({
                value: idProductoSeleccionado,
                text: productoSeleccionado?.nombre || `Producto ${idProductoSeleccionado}`
            });
        }
        // Inicializar TomSelect si aún no se ha creado.
        // Si el recurso no estuviera disponible, el select nativo sigue funcionando.
        if (typeof TomSelect === "undefined") {
            productoSelect.innerHTML = "";
            const opcionVacia = new Option(
                opcionesProducto.length ? "Seleccionar producto..." : "No hay productos disponibles",
                ""
            );
            opcionVacia.disabled = !opcionesProducto.length;
            productoSelect.appendChild(opcionVacia);
            opcionesProducto.forEach(opcion => productoSelect.add(new Option(opcion.text, opcion.value)));
            productoSelect.value = idProductoSeleccionado || "";
            return;
        }

        if (!tomSelectProducto) {
            tomSelectProducto = new TomSelect(productoSelect, {
                create: false,
                allowEmptyOption: true,
                placeholder: "Seleccionar producto...",
                maxOptions: 500,
                sortField: [{field: "text", direction: "asc"}]
            });
        }

        // Limpiar y actualizar las opciones de TomSelect
        tomSelectProducto.clear(true);
        tomSelectProducto.clearOptions();
        tomSelectProducto.addOption(opcionesProducto.length
            ? opcionesProducto
            : {value: "", text: "No hay productos disponibles", disabled: true});
        tomSelectProducto.refreshOptions(false);

        // Si hay un producto seleccionado, establecerlo como valor actual del select
        if (idProductoSeleccionado) {
            tomSelectProducto.setValue(idProductoSeleccionado);
            tomSelectProducto.refreshItems();
        } else {
            tomSelectProducto.clear(true);
        }
    }

    // Cargar los productos disponibles en la bodega desde el servidor
    async function cargarProductosBodega() {
        const response = await fetch("/hojas-produccion/productos-bodega");
        const productos = await leerRespuestaJson(response);

        if (!response.ok) {
            throw new Error("No fue posible obtener los productos.");
        }

        return Array.isArray(productos) ? productos : [];
    }

    // Limpiar el formulario de detalle
    function limpiarFormularioDetalle() {
        formularioDetalle.reset();
        limpiarValidacionesFormulario(formularioDetalle);
        idDetalleInput.value = "";
        idHojaDetalleInput.value = idHoja;
        modalDetalleTitulo.textContent = "Agregar detalle";
        // Limpiar el select de productos usando TomSelect o el valor del select nativo
        if (tomSelectProducto) {
            tomSelectProducto.clear(true);
        } else if (productoSelect) {
            productoSelect.value = "";
        }
    }

    // Renderizar la tabla de detalles con Tabulator y actualizar el footer
    function renderizarTablaDetalles(detalles) {
        const registros = Array.isArray(detalles) ? detalles : [];
        if (!tablaDetallesInstancia) {
            tablaDetallesInstancia = new Tabulator(tablaDetalles, {
                data: registros,
                layout: "fitColumns",
                responsiveLayout: "collapse",
                resizableColumns: false,
                placeholder: "No se encontraron detalles para esta hoja.",
                columnDefaults: {
                    vertAlign: "middle",
                    hozAlign: "center",
                    headerHozAlign: "center",
                    cellClick: async (event, cell) => {
                        if (soloLectura) return;
                        const elemento = event.target instanceof Element ? event.target : null;
                        if (elemento?.closest("button")) return;
                        const idDetalle = cell.getRow().getData().idDetalleHoja;
                        if (idDetalle) {
                            await abrirEdicionDetalle(idDetalle);
                        }
                    }
                },
                columns: [
                    {
                        title: "#",
                        width: 70,
                        formatter: cell => cell.getRow().getPosition(true)
                    },
                    {
                        title: "Producto",
                        field: "producto.nombre",
                        minWidth: 180,
                        formatter: cell => `<strong>${escapeHtml(cell.getValue() || "Sin producto")}</strong>`,
                        hozAlign: "left",
                        headerHozAlign: "center"
                    },
                    {
                        title: "Cantidad solicitada",
                        field: "cantidadSolicitada",
                        minWidth: 150,
                        formatter: cell => cell.getValue() ?? 0
                    },
                    {
                        title: "Cantidad surtida",
                        field: "cantidadSurtida",
                        minWidth: 140,
                        formatter: cell => cell.getValue() ?? 0
                    },
                    {
                        title: "Borrar",
                        width: 110,
                        headerSort: false,
                        formatter: () => soloLectura
                            ? '<span class="text-muted">Solo lectura</span>'
                            : '<button type="button" class="btn btn-sm btn-outline-danger" title="Eliminar detalle" data-accion="eliminar"><i class="bi bi-trash"></i></button>',
                        cellClick: async (event, cell) => {
                            const boton = event.target.closest("button[data-accion]");
                            if (!boton) return;
                            const idDetalle = cell.getRow().getData().idDetalleHoja;
                            if (!idDetalle) return;
                            if (boton.dataset.accion === "eliminar") {
                                await eliminarDetalle(idDetalle, cell.getRow().getData().producto?.nombre);
                            }
                        }
                    }
                ]
            });
        } else {
            tablaDetallesInstancia.setData(registros);
        }

        const totalPiezasSolicitadas = registros.reduce(
            (total, detalle) => total + Number(detalle.cantidadSolicitada || 0),
            0
        );
        totalProductos.textContent = String(registros.length);
        totalPiezas.textContent = String(totalPiezasSolicitadas);
        resumenDetalles.classList.remove("d-none");
    }
});

// Mostrar una alerta inicial si el contenedor principal tiene datos de alerta
function mostrarAlertaCargaInicial(contenedorVista) {
    if (!contenedorVista) {
        return;
    }
    // Obtener el tipo y mensaje de alerta desde los atributos data del contenedor
    const tipoAlerta = contenedorVista.dataset.tipoAlerta;
    const mensajeAlerta = contenedorVista.dataset.mensajeAlerta;
    // Si hay un mensaje de alerta, mostrarlo usando la función mostrarAlerta
    if (mensajeAlerta) {
        mostrarAlerta(tipoAlerta === "success" ? "success" : "error", mensajeAlerta);
    }
}

// Mostrar una alerta de éxito usando SweetAlert2
function mostrarAlerta(tipo, mensaje) {
    return tipo === "success" ? mostrarExito(mensaje) : mostrarError(mensaje);
}

// Mostrar un mensaje de éxito usando SweetAlert2
async function leerRespuestaJson(response) {
    // Leer la respuesta JSON y manejar el caso de sesión expirada (401)
    if (response.status === 401) {
        window.location.href = "/login";
        throw new Error("La sesion ha expirado.");
    }
    // Leer el contenido de la respuesta solo si es JSON
    const contentType = response.headers.get("content-type") || "";
    if (contentType.includes("application/json")) {
        return response.json();
    }

    return {};
}

// Manejar errores de fetch mostrando un mensaje de error en la consola y una alerta al usuario
function manejarErrorFetch(error, mensajePorDefecto) {
    console.error(error);
    mostrarAlerta("error", error.message || mensajePorDefecto);
}

// Escapar caracteres HTML especiales para prevenir inyección de código
function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#39;");
}
