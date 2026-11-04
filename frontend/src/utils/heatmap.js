import { localDateKey } from './time';

const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

export const levelFor = (count) => {
    if (!count) return 0;
    if (count === 1) return 1;
    if (count <= 3) return 2;
    if (count <= 5) return 3;
    return 4;
};

/**
 * GitHub-style grid: `weeks` columns (Sunday→Saturday rows) ending with the week containing `today`.
 * activity: [{ date: 'yyyy-mm-dd', count }]
 */
export const buildHeatmap = (activity = [], today = new Date(), weeks = 53) => {
    const counts = new Map(activity.map((a) => [a.date, a.count]));
    const end = new Date(today.getFullYear(), today.getMonth(), today.getDate());
    const start = new Date(end);
    start.setDate(end.getDate() - end.getDay() - (weeks - 1) * 7); // Sunday, `weeks` weeks back

    const columns = [];
    const monthLabels = [];
    let lastMonth = -1;
    const cursor = new Date(start);
    for (let w = 0; w < weeks; w += 1) {
        const column = [];
        for (let d = 0; d < 7; d += 1) {
            const key = localDateKey(cursor);
            const future = cursor > end;
            const count = future ? 0 : (counts.get(key) || 0);
            column.push({ date: key, count, level: levelFor(count), future });
            if (d === 0 && cursor.getMonth() !== lastMonth && cursor.getDate() <= 7) {
                monthLabels.push({ week: w, label: MONTHS[cursor.getMonth()] });
                lastMonth = cursor.getMonth();
            }
            cursor.setDate(cursor.getDate() + 1);
        }
        columns.push(column);
    }
    return { columns, monthLabels };
};
