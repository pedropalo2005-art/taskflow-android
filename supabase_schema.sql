-- ==============================================================
-- Esquema de Supabase para TaskFlow Pro (App Móvil de Tareas)
-- Copia y pega este script en el SQL Editor de tu panel de Supabase
-- ==============================================================

-- 1. Crear tabla de tareas (tasks)
CREATE TABLE IF NOT EXISTS public.tasks (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    description TEXT DEFAULT '',
    category TEXT DEFAULT 'General',
    status TEXT DEFAULT 'todo' CHECK (status IN ('todo', 'in_progress', 'done')),
    due_timestamp BIGINT,
    created_at BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)
);

-- 2. Habilitar Seguridad a Nivel de Fila (Row Level Security)
ALTER TABLE public.tasks ENABLE ROW LEVEL SECURITY;

-- 3. Política para permitir lectura y escritura anónima (anon key)
CREATE POLICY "Permitir acceso completo a tareas para la app"
ON public.tasks
FOR ALL
TO anon
USING (true)
WITH CHECK (true);

-- 4. Datos de ejemplo iniciales (puedes borrarlos o modificarlos)
INSERT INTO public.tasks (id, title, description, category, status, due_timestamp)
VALUES 
('task-demo-1', 'Visitar tienda Blv. Morazán', 'Revisar exhibición y cuadre de caja con encargado', '🏬 Visita Tienda', 'todo', NULL),
('task-demo-2', 'Realizar reporte de Pedidos Ya', 'Descargar pedidos de fin de semana y conciliar facturas', '📊 Reporte Pedidos', 'todo', NULL),
('task-demo-3', 'Mandar datos de venta de fin de semana', 'Enviar reporte consolidado a gerencia por WhatsApp/Correo', '💰 Ventas', 'in_progress', NULL)
ON CONFLICT (id) DO NOTHING;
