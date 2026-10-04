// Variables de estado
let surtidoEstado = null;
let procesandoEscaneo = false;
// Obtener un elemento del DOM por su ID
function obtenerElementoSurtido(id) {
    return document.getElementById(id);
}
// Leer la respuesta de una solicitud fetch y manejar errores de sesión
async function leerRespuestaSurtido(response) {
    if (response.status === 401) {
        window.location.href = '/login';
        throw new Error('La sesión ha expirado.');
    }
    // Leer el contenido de la respuesta como texto y luego intentar parsearlo como JSON
    const texto = await response.text();
    if (!texto) return {};
    try {
        return JSON.parse(texto);
    } catch {
        return {mensaje: texto};
    }
}
// Generar un mensaje de error amigable basado en el mensaje original del error
function mensajeNegocioSurtido(error) {
    // Obtener el mensaje original del error y convertirlo a minúsculas para comparaciones
    const original = String(error?.message || error || '').trim();
    const mensaje = original.toLowerCase();
    // Comparar el mensaje con patrones conocidos y devolver un mensaje amigable
    if (mensaje.includes('salida pendiente') || mensaje.includes('ya tiene una salida')) {
        return 'No se puede escanear: este conjunto ya salió a llamado y aún no ha regresado.';
    }
    if (mensaje.includes('no pertenece a la bodega')) {
        return 'No se puede escanear: el conjunto no pertenece a la bodega activa.';
    }
    if (mensaje.includes('conjunto no existe')) {
        return 'No se puede escanear: el conjunto no existe o el código no es válido.';
    }
    if (mensaje.includes('no está disponible para salida')) {
        return 'No se puede escanear: el conjunto no está disponible. Puede estar fuera de la bodega, en mantenimiento o ya haber salido a llamado.';
    }
    if (mensaje.includes('producto del conjunto no coincide')) {
        return 'No se puede escanear: el conjunto no corresponde a ningún producto solicitado en esta hoja.';
    }
    if (mensaje.includes('no existe detalle')) {
        return 'No se puede escanear: el producto del conjunto no está solicitado en esta hoja.';
    }
    if (mensaje.includes('cantidad solicitada ya fue surtida')) {
        return 'No se puede escanear: la cantidad solicitada de este producto ya fue surtida.';
    }
    if (mensaje.includes('capacidad del contenedor')) {
        return 'No se puede escanear: el contenedor ya alcanzó su capacidad.';
    }
    if (mensaje.includes('carga del contenedor no está abierta')) {
        return 'No se puede escanear: la carga de este contenedor ya fue cerrada o liberada.';
    }
    if (mensaje.includes('contenedor no está disponible')) {
        return 'No se puede asignar: el contenedor no está disponible para una nueva carga.';
    }
    if (mensaje.includes('contenedor ya tiene una asignación activa')) {
        return 'No se puede asignar: el contenedor ya está siendo utilizado en otra hoja.';
    }
    if (mensaje.includes('hoja no pertenece a la bodega')) {
        return 'No se puede operar: la hoja no pertenece a la bodega activa.';
    }
    if (mensaje.includes('no está disponible para surtido')
            || mensaje.includes('no está disponible para registrar salidas')) {
        return 'No se puede operar: la hoja debe estar en estado Por surtir o Surtiendo.';
    }
    if (mensaje.includes('hoja de producción no existe')) {
        return 'No se puede operar: la hoja de producción no existe.';
    }
    if (mensaje.includes('carga vacía')) {
        return 'No se puede cerrar la carga porque todavía no tiene conjuntos surtidos.';
    }
    return original || 'No fue posible completar la operación.';
}
// Actualizar el texto de un elemento del DOM por su ID
function actualizarTextoSurtido(id, valor) {
    const elemento = obtenerElementoSurtido(id);
    if (elemento) elemento.textContent = valor ?? '';
}
// Mostrar la sección de operación de surtido y ocultar la selección de hoja
function mostrarOperacionSurtido() {
    obtenerElementoSurtido('seccionSeleccionHoja')?.classList.add('d-none');
    obtenerElementoSurtido('seccionOperacionSurtido')?.classList.remove('d-none');
}

// Mostrar la sección de selección de hoja y ocultar la operación de surtido
function mostrarSeleccionHojaSurtido() {
    // Ocultar la sección de operación de surtido y mostrar la selección de hoja
    obtenerElementoSurtido('seccionOperacionSurtido')?.classList.add('d-none');
    const seleccion = obtenerElementoSurtido('seccionSeleccionHoja');
    seleccion?.classList.remove('d-none');
    // Limpiar el valor del input de selección de hoja y enfocarlo
    const input = obtenerElementoSurtido('idHojaSeleccionada');
    if (input) {
        input.value = '';
        input.focus();
    }
}
// Actualizar el estado de surtido y renderizar la información en la interfaz
function actualizarEstadoSurtido(estado) {
    // Guardar el estado actual de surtido
    surtidoEstado = estado;
    mostrarOperacionSurtido();
    // Actualizar los textos de la interfaz con la información del estado
    actualizarTextoSurtido('tituloSurtidoHoja', estado.nombreProyecto || 'Sin proyecto');
    actualizarTextoSurtido('hhCliente', estado.cliente || 'Sin cliente');
    actualizarTextoSurtido('hhFechaSalida', formatearFechaSurtido(estado.fechaSalida));
    actualizarTextoSurtido('hhPorcentaje', estado.porcentaje ?? 0);
    actualizarTextoSurtido('hhTotalSolicitado', estado.totalSolicitado ?? 0);
    actualizarTextoSurtido('hhTotalSurtido', estado.totalSurtido ?? 0);
    actualizarTextoSurtido('hhTotalPendiente', estado.totalPendiente ?? 0);
    // Actualizar la barra de progreso con el porcentaje de surtido
    const porcentaje = Math.max(0, Math.min(100, Number(estado.porcentaje) || 0));
    const progreso = obtenerElementoSurtido('hhProgreso');
    // Actualizar la barra de progreso y el texto de porcentaje si el elemento existe
    if (progreso) {
        progreso.style.width = `${porcentaje}%`;
        progreso.textContent = `${porcentaje}%`;
        progreso.setAttribute('aria-valuenow', String(porcentaje));
    }
    // Actualizar la información del contenedor activo, detalles y movimientos recientes
    actualizarContenedorSurtido(estado.contenedorActivo);
    renderizarDetallesSurtido(estado.detalles || []);
    renderizarMovimientosSurtido(estado.ultimosMovimientos || []);
    enfocarEntradaSurtido();
}

// Enfocar el input correspondiente según el estado del contenedor activo
function enfocarEntradaSurtido() {
    window.requestAnimationFrame(() => {
        const idElemento = surtidoEstado?.contenedorActivo?.disponible
            ? 'codigoConjuntoHH'
            : 'codigoContenedorHH';
        const elemento = obtenerElementoSurtido(idElemento);
        if (elemento && !elemento.disabled && !elemento.classList.contains('d-none')) {
            elemento.focus();
            elemento.select?.();
        }
    });
}

// Actualizar la información del contenedor activo en la interfaz
function actualizarContenedorSurtido(contenedor) {
    // Mostrar u ocultar el bloque de asignación según si hay un contenedor activo
    const bloqueAsignar = obtenerElementoSurtido('bloqueAsignarContenedor');
    if (bloqueAsignar) bloqueAsignar.classList.toggle('d-none', Boolean(contenedor));
    // Mostrar u ocultar el bloque de información del contenedor según si hay un contenedor activo
    const codigo = contenedor?.codigo || 'Ninguno';
    // Actualizar los textos de la interfaz con la información del contenedor activo
    actualizarTextoSurtido('hhCodigoContenedor', codigo);
    actualizarTextoSurtido('hhOcupacionContenedor', contenedor?.ocupacion ?? 0);
    actualizarTextoSurtido('hhCapacidadContenedor', contenedor?.capacidad ?? 0);
    // Actualizar la barra de progreso del contenedor con el porcentaje de ocupación
    const porcentaje = Math.max(0, Math.min(100, Number(contenedor?.porcentaje) || 0));
    const barra = obtenerElementoSurtido('hhProgresoContenedor');
    if (barra) barra.style.width = `${porcentaje}%`;
    // Actualizar el estado del contenedor con un badge de color según su disponibilidad y ocupación
    const estado = obtenerElementoSurtido('hhEstadoContenedor');
    if (estado) {
        estado.className = `badge ${contenedor
            ? (contenedor.disponible ? 'text-bg-success' : 'text-bg-warning')
            : 'text-bg-secondary'}`;
        estado.textContent = !contenedor
            ? 'Sin asignar'
            : contenedor.lleno ? 'Lleno' : contenedor.cargaCerrada ? 'Carga cerrada' : 'Activo';
    }
    // Habilitar o deshabilitar el input de escaneo de conjunto y el botón de cerrar carga según la disponibilidad del contenedor
    const inputConjunto = obtenerElementoSurtido('codigoConjuntoHH');
    const btnCerrar = obtenerElementoSurtido('btnCerrarCarga');
    // Determinar si se puede escanear un conjunto según la disponibilidad del contenedor
    const puedeEscanear = Boolean(contenedor?.disponible);
    if (inputConjunto) {
        inputConjunto.disabled = !puedeEscanear;
        inputConjunto.placeholder = puedeEscanear
            ? 'Escanea CON-000001'
            : 'Asigna un contenedor disponible';
    }
    // Determinar si se puede cerrar la carga según la disponibilidad y ocupación del contenedor
    if (btnCerrar) {
        btnCerrar.disabled = !contenedor || Boolean(contenedor.cargaCerrada)
            || Number(contenedor.ocupacion) <= 0;
    }
}
// Renderizar los detalles de surtido en la interfaz
function renderizarDetallesSurtido(detalles) {
    // Obtener el contenedor de detalles y limpiar su contenido
    const contenedor = obtenerElementoSurtido('hhDetalles');
    // Si no existe el contenedor, salir de la función
    if (!contenedor) return;
    // Limpiar el contenido del contenedor antes de renderizar los detalles
    contenedor.replaceChildren();
    // Si no hay detalles, mostrar un placeholder indicando que no hay detalles registrados
    if (!detalles.length) {
        contenedor.appendChild(crearPlaceholderSurtido('La hoja no tiene detalles registrados.'));
        return;
    }
    // Iterar sobre cada detalle y crear una tarjeta con la información correspondiente
    detalles.forEach(detalle => {
        // Crear un div para la tarjeta del detalle con clases de estilo
        const tarjeta = document.createElement('div');
        tarjeta.className = 'border rounded p-3';
        // Crear el encabezado de la tarjeta con el nombre del producto y el avance de surtido
        const encabezado = document.createElement('div');
        encabezado.className = 'd-flex justify-content-between gap-3';
        const producto = document.createElement('strong');
        producto.textContent = detalle.producto || 'Producto sin nombre';
        const avance = document.createElement('span');
        avance.className = 'text-muted small';
        // Mostrar la cantidad surtida y la cantidad solicitada en el formato "surtida / solicitada"
        avance.textContent = `${detalle.cantidadSurtida ?? 0} / ${detalle.cantidadSolicitada ?? 0}`;
        encabezado.append(producto, avance);
        // Crear la barra de progreso con el porcentaje de surtido y un badge de color según si está completo o pendiente
        const barra = document.createElement('div');
        barra.className = 'progress mt-2';
        barra.setAttribute('role', 'progressbar');
        barra.setAttribute('aria-label', `Avance de ${detalle.producto || 'producto'}`);
        const barraInterna = document.createElement('div');
        barraInterna.className = `progress-bar ${detalle.completo ? 'bg-success' : ''}`;
        const porcentaje = Math.max(0, Math.min(100, Number(detalle.porcentaje) || 0));
        barraInterna.style.width = `${porcentaje}%`;
        barraInterna.textContent = `${porcentaje}%`;
        barra.appendChild(barraInterna);

        // Crear el pie de la tarjeta con el estado de completitud o la cantidad pendiente
        const pie = document.createElement('small');
        pie.className = 'text-muted d-block mt-2';
        // Mostrar "Completo" si el detalle está completo, o la cantidad pendiente si no lo está
        pie.textContent = detalle.completo
            ? 'Completo'
            : `Pendiente: ${detalle.cantidadPendiente ?? 0}`;
        // Agregar el encabezado, la barra de progreso y el pie a la tarjeta
        tarjeta.append(encabezado, barra, pie);
        // Agregar la tarjeta al contenedor de detalles
        contenedor.appendChild(tarjeta);
    });
}
// Renderizar los movimientos recientes de surtido en la interfaz
function renderizarMovimientosSurtido(movimientos) {
    // Obtener el contenedor de movimientos y limpiar su contenido
    const contenedor = obtenerElementoSurtido('hhMovimientos');
    if (!contenedor) return;
    contenedor.replaceChildren();
    // Si no hay movimientos, mostrar un placeholder indicando que no hay movimientos recientes
    if (!movimientos.length) {
        contenedor.appendChild(crearPlaceholderSurtido('No hay movimientos recientes.'));
        return;
    }
    // Iterar sobre cada movimiento y crear un div con la información correspondiente
    movimientos.forEach(movimiento => {
        const fila = document.createElement('div');
        fila.className = 'border rounded p-2';
        const encabezado = document.createElement('div');
        encabezado.className = 'd-flex justify-content-between gap-2';
        const conjunto = document.createElement('strong');
        conjunto.textContent = movimiento.codigoConjunto || 'Conjunto';
        const fecha = document.createElement('small');
        fecha.className = 'text-muted';
        fecha.textContent = formatearFechaSurtido(movimiento.fecha);
        encabezado.append(conjunto, fecha);
        // Crear un pequeño detalle con el producto, contenedor y usuario del movimiento
        const detalle = document.createElement('small');
        detalle.className = 'text-muted';
        detalle.textContent = `${movimiento.producto || 'Producto'} · ${movimiento.codigoContenedor || 'Sin contenedor'} · ${movimiento.usuario || 'Usuario'}`;
        fila.append(encabezado, detalle);
        contenedor.appendChild(fila);
    });
}
// Crear un elemento placeholder con un mensaje dado
function crearPlaceholderSurtido(texto) {
    const elemento = document.createElement('div');
    elemento.className = 'hh-placeholder';
    elemento.textContent = texto;
    return elemento;
}
// Formatear una fecha en formato local de México o devolver un mensaje si no hay fecha
function formatearFechaSurtido(valor) {
    if (!valor) return 'Sin fecha';
    const fecha = new Date(valor);
    return Number.isNaN(fecha.getTime())
        ? String(valor)
        : fecha.toLocaleString('es-MX', {
            day: '2-digit', month: '2-digit', year: 'numeric',
            hour: '2-digit', minute: '2-digit'
        });
}
// Cargar el estado de surtido desde el servidor y actualizar la interfaz
async function cargarEstadoSurtido() {
    // Verificar que el ID de la hoja de surtido sea un número entero válido antes de hacer la solicitud
    if (!Number.isInteger(idHojaSurtido)) return;
    // Hacer una solicitud fetch al endpoint de estado de surtido para la hoja actual
    const response = await fetch(`/hh/surtido/api/hojas/${idHojaSurtido}/estado`);
    // Leer la respuesta y manejar errores de sesión o de negocio
    const data = await leerRespuestaSurtido(response);
    if (!response.ok) throw new Error(data.mensaje || 'No fue posible cargar el estado de surtido.');
    actualizarEstadoSurtido(data);
}
// Asignar un contenedor a la hoja de surtido actual mediante una solicitud POST al servidor
async function asignarContenedorSurtido() {
    // Obtenemos el input del código del contenedor y verificamos que tenga un valor válido
    const input = obtenerElementoSurtido('codigoContenedorHH');
    const codigo = input?.value.trim();
    if (!codigo) {
        mostrarError('Captura o escanea el código del contenedor.', 'Contenedor requerido');
        input?.focus();
        return;
    }
    // Intentamos asignar el contenedor mediante una solicitud POST al servidor
    try {
        LoadingOverlay.mostrar('Asignando contenedor...');
        const response = await fetch(`/hh/surtido/api/hojas/${idHojaSurtido}/contenedores`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({codigo})
        });
        const data = await leerRespuestaSurtido(response);
        if (!response.ok) throw new Error(data.mensaje || 'No fue posible asignar el contenedor.');
        // Limpiamos el input del código del contenedor y recargamos el estado de surtido
        input.value = '';
        await cargarEstadoSurtido();
        await LoadingOverlay.ocultar();
        // Mostramos un mensaje de éxito temporal y enfocamos el input de escaneo de conjunto
        await mostrarExitoTemporal(data.mensaje || 'Contenedor asignado correctamente.', 'Contenedor asignado');
        obtenerElementoSurtido('codigoConjuntoHH')?.focus();
    } catch (error) {
        await LoadingOverlay.ocultar();
        const mensaje = mensajeNegocioSurtido(error);
        await mostrarError(mensaje, 'No fue posible asignar');
    }
}
// Registrar la salida de un conjunto mediante una solicitud POST al servidor
async function registrarSalidaSurtido() {
    // Verificamos si ya se está procesando un escaneo para evitar solicitudes duplicadas
    if (procesandoEscaneo) return;
    // Verificamos que haya un contenedor activo asignado antes de registrar la salida
    if (!surtidoEstado?.contenedorActivo?.idHojaContenedor) {
        const mensaje = 'No se puede escanear: primero asigna un contenedor activo.';
        mostrarError(mensaje, 'Contenedor requerido');
        return;
    }
    // Verificamos si la hoja ya está completamente surtida antes de registrar la salida
    if (surtidoEstado.completo) {
        const mensaje = 'No se puede escanear: la hoja ya está completamente surtida.';
        mostrarError(mensaje, 'Hoja completa');
        return;
    }
    // Obtenemos el input del código del conjunto y verificamos que tenga un valor válido
    const input = obtenerElementoSurtido('codigoConjuntoHH');
    const codigoConjunto = input?.value.trim();
    if (!codigoConjunto) return;
    // Marcamos que se está procesando un escaneo y deshabilitamos el input para evitar interacciones mientras se procesa
    procesandoEscaneo = true;
    input.disabled = true;
    // Intentamos registrar la salida mediante una solicitud POST al servidor
    try {
        LoadingOverlay.mostrar('Registrando salida...');
        const response = await fetch(`/hh/surtido/api/hojas/${idHojaSurtido}/salidas`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({
                codigoConjunto,
                idHojaContenedor: surtidoEstado.contenedorActivo.idHojaContenedor
            })
        });
        const data = await leerRespuestaSurtido(response);
        if (!response.ok) throw new Error(data.mensaje || 'No fue posible registrar la salida.');
        // Limpiamos el input del código del conjunto y recargamos el estado de surtido
        input.value = '';
        await cargarEstadoSurtido();
        await LoadingOverlay.ocultar();
        await mostrarExitoTemporal(data.mensaje || 'Salida registrada correctamente.', 'Salida registrada');
    } catch (error) {
        await LoadingOverlay.ocultar();
        const mensaje = mensajeNegocioSurtido(error);
        await mostrarError(mensaje, 'No fue posible registrar la salida');
    } finally {
        // Marcamos que ya no se está procesando un escaneo y habilitamos el input si el contenedor sigue disponible
        procesandoEscaneo = false;
        if (surtidoEstado?.contenedorActivo?.disponible) {
            input.disabled = false;
            input.focus();
        }
    }
}
// Cerrar la carga del contenedor activo mediante una solicitud POST al servidor
async function cerrarCargaSurtido() {
    // Verificamos que haya un contenedor activo asignado antes de cerrar la carga
    const contenedor = surtidoEstado?.contenedorActivo;
    if (!contenedor || !contenedor.idHojaContenedor) return;
    // Mostramos un cuadro de confirmación al usuario antes de cerrar la carga
    const confirmacion = await confirmarAccion({
        titulo: '¿Cerrar carga?',
        mensaje: 'Después del cierre no se podrán registrar más salidas en este contenedor.',
        textoConfirmar: 'Sí, cerrar carga',
        colorConfirmar: '#0d6efd'
    });
    if (!confirmacion.isConfirmed) return;
    // Intentamos cerrar la carga mediante una solicitud POST al servidor
    try {
        LoadingOverlay.mostrar('Cerrando carga...');
        const response = await fetch(
            `/hh/surtido/api/hojas/${idHojaSurtido}/contenedores/${contenedor.idHojaContenedor}/cerrar`,
            {method: 'POST'}
        );
        const data = await leerRespuestaSurtido(response);
        if (!response.ok) throw new Error(data.mensaje || 'No fue posible cerrar la carga.');
        // Si la hoja está completamente surtida, mostramos un mensaje de éxito y redirigimos al listado de hojas
        if (data.surtidoCompleto) {
            await LoadingOverlay.ocultar();
            await mostrarExitoTemporal(
                data.mensaje || 'Surtido completado. La hoja cambió al estado En llamado.',
                'Surtido completado',
                2200
            );
            window.location.href = '/hh/surtido';
            return;
        }
        // Si la hoja no está completamente surtida, recargamos el estado de surtido
        await cargarEstadoSurtido();
        await LoadingOverlay.ocultar();
        await mostrarExitoTemporal(data.mensaje || 'Carga cerrada correctamente.', 'Carga cerrada');
    } catch (error) {
        await LoadingOverlay.ocultar();
        const mensaje = mensajeNegocioSurtido(error);
        await mostrarError(mensaje, 'No fue posible cerrar la carga');
    }
}
// Variable para almacenar el ID de la hoja de surtido actual
let idHojaSurtido = null;
// Configuración de eventos al cargar el DOM
document.addEventListener('DOMContentLoaded', () => {
    // Obtener el ID de la hoja de surtido desde el atributo data-id-hoja del elemento <main>
    const principal = document.querySelector('main[data-id-hoja]');
    // Convertir el valor a número y validar que sea un entero positivo
    idHojaSurtido = Number(principal?.dataset.idHoja);
    if (!Number.isInteger(idHojaSurtido) || idHojaSurtido <= 0) {
        idHojaSurtido = null;
    }
    // Agregar un evento de submit al formulario de selección de hoja para redirigir a la hoja seleccionada
    const formSeleccion = obtenerElementoSurtido('formSeleccionHoja');
    formSeleccion?.addEventListener('submit', event => {
        // Evitar el comportamiento por defecto del formulario
        event.preventDefault();
        // Obtener el valor del input de selección de hoja y convertirlo a número
        const idHoja = Number(obtenerElementoSurtido('idHojaSeleccionada')?.value);
        // Validar que el ID de hoja sea un número entero positivo antes de redirigir
        if (!Number.isInteger(idHoja) || idHoja <= 0) {
            formSeleccion.classList.add('was-validated');
            return;
        }
        // Redirigir a la página de surtido de la hoja seleccionada
        window.location.href = `/hh/surtido/${idHoja}`;
    });
    // Agregar eventos de click y keydown a los botones e inputs correspondientes para asignar contenedor, registrar salida y cerrar carga
    obtenerElementoSurtido('btnAsignarContenedor')?.addEventListener('click', asignarContenedorSurtido);
    obtenerElementoSurtido('codigoContenedorHH')?.addEventListener('keydown', event => {
        if (event.key === 'Enter') {
            event.preventDefault();
            asignarContenedorSurtido();
        }
    });
    obtenerElementoSurtido('codigoConjuntoHH')?.addEventListener('keydown', event => {
        if (event.key === 'Enter') {
            event.preventDefault();
            registrarSalidaSurtido();
        }
    });
    obtenerElementoSurtido('btnCerrarCarga')?.addEventListener('click', cerrarCargaSurtido);

    // Si hay un ID de hoja de surtido válido, cargamos su estado
    if (idHojaSurtido) {
        LoadingOverlay.ejecutar(cargarEstadoSurtido, 'Cargando estado de surtido...')
            .catch(async error => {
                const mensaje = mensajeNegocioSurtido(error);
                await mostrarError(mensaje, 'No fue posible cargar la hoja');
                mostrarSeleccionHojaSurtido();
            });
    }
});
