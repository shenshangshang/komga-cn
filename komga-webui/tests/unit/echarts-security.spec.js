const fs = require('fs')
const path = require('path')

describe('ECharts tooltip security', () => {
  const charts = [
    'EChartsBarChart.vue',
    'EChartsLineChart.vue',
    'EChartsPieChart.vue',
    'EChartsWordCloud.vue',
  ]

  it.each(charts)('%s renders tooltip values as canvas text', file => {
    const source = fs.readFileSync(path.join(process.cwd(), 'src/components', file), 'utf8')
    const tooltipBlocks = [...source.matchAll(/tooltip:\s*\{([\s\S]*?)\n\s*\},/g)]

    expect(tooltipBlocks.length).toBeGreaterThan(0)
    expect(tooltipBlocks.every(([, body]) => body.includes('renderMode: \'richText\''))).toBe(true)
  })
})
