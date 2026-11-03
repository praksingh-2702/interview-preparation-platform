import React from 'react';

const Brand = ({ as: Tag = 'span', className = '' }) => (
    <Tag className={`brand ${className}`}>
        FreakCode <span className="brand-accent">Top 100</span>
    </Tag>
);

export default Brand;
