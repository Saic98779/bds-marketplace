import { Bar, BarChart, CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis, LabelList } from 'recharts';
import { useState } from 'react';
import { Icon } from './ui';

const SERIES = ['var(--series-1)', 'var(--series-2)', 'var(--series-3)'];
const SERIES_HEX = ['#2a78d6', '#eb6834', '#1baf7a'];
const AXIS = { fontSize: 12, fill: '#5a7083' };

function ChartTooltip({ active, payload, label: title }) {
  if (!active || !payload?.length) return null;
  return (
    <div className="viz-tooltip">
      <div className="t">{title}</div>
      {payload.map((p) => (
        <div key={p.dataKey} className="row gap-8">
          <span className="sw" style={{ width: 10, height: 10, borderRadius: 3, background: p.color, display: 'inline-block' }} />
          <span className="grow">{p.name}</span>
          <span className="bold">{p.value}</span>
        </div>
      ))}
    </div>
  );
}

function TableView({ data, xKey, series }) {
  return (
    <div className="table-wrap" style={{ maxHeight: 260, overflowY: 'auto' }}>
      <table className="table">
        <thead><tr><th>{xKey === 'period' ? 'Period' : 'Label'}</th>{series.map((s) => <th key={s.key} className="right">{s.name}</th>)}</tr></thead>
        <tbody>{data.map((row) => <tr key={row[xKey]}><td>{row[xKey]}</td>{series.map((s) => <td key={s.key} className="right">{row[s.key]}</td>)}</tr>)}</tbody>
      </table>
    </div>
  );
}

export function TrendChart({ title, subtitle, data = [], xKey = 'period', series }) {
  const [table, setTable] = useState(false);
  return (
    <div className="card chart-card">
      <div className="card-header">
        <div>
          <h3>{title}</h3>
          {subtitle && <div className="tiny muted">{subtitle}</div>}
        </div>
        <div className="row gap-16">
          {series.length > 1 && (
            <div className="chart-legend">
              {series.map((s, i) => <span key={s.key}><span className="sw" style={{ background: SERIES[i] }} />{s.name}</span>)}
            </div>
          )}
          <button className="btn btn-ghost btn-sm" onClick={() => setTable(!table)} aria-label="Toggle table view">
            <Icon name={table ? 'show_chart' : 'table_rows'} size="sm" />
          </button>
        </div>
      </div>
      <div className="card-body">
        {table ? <TableView data={data} xKey={xKey} series={series} /> : (
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={data} margin={{ top: 16, right: 16, left: -12, bottom: 0 }}>
              <CartesianGrid stroke="#e2e9ef" vertical={false} />
              <XAxis dataKey={xKey} tick={AXIS} axisLine={{ stroke: '#cbd8e2' }} tickLine={false} />
              <YAxis tick={AXIS} axisLine={false} tickLine={false} allowDecimals={false} />
              <Tooltip content={<ChartTooltip />} cursor={{ stroke: '#8a9ba8', strokeDasharray: '3 3' }} />
              {series.map((s, i) => (
                <Line key={s.key} type="monotone" dataKey={s.key} name={s.name} stroke={SERIES_HEX[i]} strokeWidth={2}
                  dot={{ r: 4, strokeWidth: 2, stroke: '#fff', fill: SERIES_HEX[i] }} activeDot={{ r: 6, stroke: '#fff', strokeWidth: 2 }} />
              ))}
            </LineChart>
          </ResponsiveContainer>
        )}
      </div>
    </div>
  );
}

export function ColumnChart({ title, subtitle, data = [], xKey = 'period', valueKey = 'value', name = 'Count' }) {
  return (
    <div className="card chart-card">
      <div className="card-header"><div><h3>{title}</h3>{subtitle && <div className="tiny muted">{subtitle}</div>}</div></div>
      <div className="card-body">
        <ResponsiveContainer width="100%" height="100%">
          <BarChart data={data} margin={{ top: 20, right: 8, left: -18, bottom: 0 }} barCategoryGap="28%">
            <CartesianGrid stroke="#e2e9ef" vertical={false} />
            <XAxis dataKey={xKey} tick={AXIS} axisLine={{ stroke: '#cbd8e2' }} tickLine={false} />
            <YAxis tick={AXIS} axisLine={false} tickLine={false} allowDecimals={false} />
            <Tooltip content={<ChartTooltip />} cursor={{ fill: 'rgba(42,120,214,.06)' }} />
            <Bar dataKey={valueKey} name={name} fill={SERIES_HEX[0]} radius={[4, 4, 0, 0]} maxBarSize={36}>
              <LabelList dataKey={valueKey} position="top" style={{ fontSize: 11.5, fill: '#2e4a5f' }} />
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}

export function BarList({ title, subtitle, items = [], empty = 'No data yet', format = (v) => v }) {
  const max = Math.max(1, ...items.map((i) => i.count));
  return (
    <div className="card">
      <div className="card-header"><div><h3>{title}</h3>{subtitle && <div className="tiny muted">{subtitle}</div>}</div></div>
      <div className="card-body">
        {items.length === 0 ? <p className="muted small">{empty}</p> : (
          <div className="bar-list">
            {items.map((i) => (
              <div className="bar-row" key={i.label} title={`${i.label}: ${i.count}`}>
                <span className="small" style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{i.label}</span>
                <div className="track"><div style={{ width: `${(i.count / max) * 100}%` }} /></div>
                <span className="small bold right">{format(i.count)}</span>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
